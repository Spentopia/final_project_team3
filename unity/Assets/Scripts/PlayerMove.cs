using UnityEngine;
using UnityEngine.InputSystem;
using Unity.Cinemachine;
using System.Collections;
using System.Collections.Generic;
using FishNet.Object;
using FishNet.Object.Synchronizing;

public class PlayerMove : NetworkBehaviour 
{
    [Header("이동 설정")]
    public float moveSpeed = 10f;
    public float jumpForce = 12f;
    
    [Header("조종 키 설정")]
    public Key leftKey = Key.LeftArrow;
    public Key rightKey = Key.RightArrow;
    public Key jumpKey = Key.LeftAlt;
    public Key downKey = Key.DownArrow;

    private Rigidbody2D rb;
    private bool isGrounded;
    private Animator anim; 
    private Collider2D playerCollider;

    // ==========================================
    // 🌟 FishNet V4 최신 문법 (스케일 동기화)
    // ==========================================
    private readonly SyncVar<float> syncScaleX = new SyncVar<float>(1f);

    private void OnScaleXChanged(float oldVal, float newVal, bool asServer)
    {
        transform.localScale = new Vector3(newVal, 1f, 1f);
    }

    [ServerRpc]
    private void CmdUpdateScale(float newScaleX)
    {
        syncScaleX.Value = newScaleX; // V4에서는 .Value에 값을 넣습니다!
    }
    // ==========================================

    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        anim = GetComponent<Animator>();
        playerCollider = GetComponent<Collider2D>();
        
        rb.sharedMaterial = new PhysicsMaterial2D("Slippery") { friction = 0f, bounciness = 0f };

        // V4 동기화 이벤트 연결
        syncScaleX.OnChange += OnScaleXChanged;
    }

    public override void OnStartNetwork()
    {
        base.OnStartNetwork();

        rb.gravityScale = 0f;
        rb.bodyType = RigidbodyType2D.Kinematic;
        StartCoroutine(EnablePhysicsDelayed());
    }

    public override void OnStartClient()
    {
        base.OnStartClient();

        if (base.IsOwner)
        {
            var vcam = GameObject.FindAnyObjectByType<CinemachineCamera>();
            if (vcam != null) {
                vcam.Target.TrackingTarget = transform; 
                var composer = vcam.GetComponent<CinemachinePositionComposer>();
                if (composer != null) composer.Composition.ScreenPosition = new Vector2(0.5f, 0.2f);
            }
        }
    }

    private IEnumerator EnablePhysicsDelayed()
    {
        yield return new WaitForSeconds(0.5f);
        
        if (base.IsOwner) 
        {
            rb.gravityScale = 3f; 
            rb.bodyType = RigidbodyType2D.Dynamic;
        }
    }

    void Update()
    {
        if (!base.IsOwner) return;

        float moveInput = 0f;

        // 🌟 [수정됨] 무조건 return 해버리지 않고, 채팅 중이 아닐 때만 입력을 받도록 변경
        if (!GlobalChatManager.IsChatFocused && Keyboard.current != null) 
        {
            if (Keyboard.current[leftKey].isPressed) moveInput = -1f;
            else if (Keyboard.current[rightKey].isPressed) moveInput = 1f;
        }
        
        // 🌟 이제 채팅창을 누르면 moveInput이 0이 되므로 정상적으로 멈춥니다!
        if (rb.bodyType == RigidbodyType2D.Dynamic)
        {
            rb.linearVelocity = new Vector2(moveInput * moveSpeed, rb.linearVelocity.y);
        }

        if (anim != null) anim.SetBool("isWalking", moveInput != 0);

        if (moveInput != 0) 
        {
            float targetScaleX = moveInput > 0 ? -1f : 1f;
            
            if (transform.localScale.x != targetScaleX)
            {
                transform.localScale = new Vector3(targetScaleX, 1f, 1f);
                CmdUpdateScale(targetScaleX);
            }
        }

        // 🌟 점프도 채팅 중이 아닐 때만 작동하도록 방어
        if (!GlobalChatManager.IsChatFocused && Keyboard.current != null && Keyboard.current[jumpKey].wasPressedThisFrame && isGrounded)
        {
            if (Keyboard.current[downKey].isPressed) {
                StartCoroutine(DropThrough());
            } else {
                rb.AddForce(Vector2.up * jumpForce, ForceMode2D.Impulse);
            }
        }
    }

    // ==========================================
    // 🌟 [추가됨] 창을 내리거나 알트탭 했을 때 캐릭터가 계속 가는 버그 방지
    // ==========================================
    private void OnApplicationFocus(bool hasFocus)
    {
        // [보호막 1] FishNet의 네트워크 오브젝트 정보 자체가 메모리에 있는지 확인
        // (씬 전환 시 이 스크립트가 붙은 객체가 파괴 중일 때를 대비)
        if (this == null || NetworkObject == null)
            return;

        // [보호막 2] 서버로부터 정상적으로 연동(Spawn)되었는지 확인
        if (!IsSpawned)
            return;

        // [보호막 3] 내 캐릭터일 때만 로직 실행
        if (!IsOwner)
            return;

        // 실제 로직
        if (!hasFocus && rb != null)
        {
            rb.linearVelocity = new Vector2(0f, rb.linearVelocity.y);
            if (anim != null) anim.SetBool("isWalking", false);
        }
    }

    IEnumerator DropThrough()
    {
        ContactPoint2D[] contacts = new ContactPoint2D[10];
        int contactCount = playerCollider.GetContacts(contacts);
        List<Collider2D> platforms = new List<Collider2D>();

        for (int i = 0; i < contactCount; i++) {
            if (contacts[i].collider.gameObject.layer == LayerMask.NameToLayer("Platform")) {
                platforms.Add(contacts[i].collider);
            }
        }

        if (platforms.Count == 0)
        {
            RaycastHit2D hit = Physics2D.Raycast(transform.position, Vector2.down, 1.0f, LayerMask.GetMask("Platform"));
            if (hit.collider != null) platforms.Add(hit.collider);
        }

        foreach (var plat in platforms) {
            Physics2D.IgnoreCollision(playerCollider, plat, true);
        }

        rb.linearVelocity = new Vector2(rb.linearVelocity.x, -jumpForce * 0.5f);

        yield return new WaitForSeconds(0.25f);

        foreach (var plat in platforms)
        {
            if (plat == null) continue;
            while (transform.position.y > plat.bounds.min.y)
            {
                yield return null; 
            }
        }

        foreach (var plat in platforms) {
            if (plat != null)
                Physics2D.IgnoreCollision(playerCollider, plat, false);
        }
    }

    private void OnCollisionStay2D(Collision2D collision) {
        if (collision.gameObject.CompareTag("Ground") || collision.gameObject.CompareTag("Platform")) 
            isGrounded = true;
    }

    private void OnCollisionExit2D(Collision2D collision) {
        if (collision.gameObject.CompareTag("Ground") || collision.gameObject.CompareTag("Platform")) 
            isGrounded = false;
    }
}