using UnityEngine;
using FishNet.Object;

public class SmartPortal : MonoBehaviour
{
    [Header("이동할 목적지 위치 (Transform)")]
    public Transform destination;

    [Header("이동할 타겟 맵 오브젝트 (GameObject)")]
    public GameObject targetMap;

    private bool isInside = false;
    private GameObject localPlayerInPortal = null;

    // 🌟 [추가] 텔레포트 후 호출하여 포탈 인식을 강제로 갱신/초기화하는 함수
    public void RefreshPortalStatus(GameObject player)
    {
        Collider2D myCollider = GetComponent<Collider2D>();
        Collider2D playerCollider = player.GetComponent<Collider2D>();

        // 캐릭터가 현재 내 포탈 트리거 범위 안에 물리적으로 있는지 다시 확인
        if (myCollider != null && playerCollider != null && myCollider.IsTouching(playerCollider))
        {
            isInside = true;
            localPlayerInPortal = player;
            Debug.Log($"[SmartPortal] {name}이(가) {player.name}을(를) 포탈 안으로 인식했습니다.");
        }
    }

    private void OnTriggerEnter2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
        {
            var nob = other.GetComponent<NetworkObject>();
            if (nob != null && nob.IsOwner)
            {
                isInside = true;
                localPlayerInPortal = other.gameObject;
            }
        }
    }

    private void OnTriggerExit2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
        {
            var nob = other.GetComponent<NetworkObject>();
            if (nob != null && nob.IsOwner)
            {
                isInside = false;
                localPlayerInPortal = null;
            }
        }
    }

    private void Update()
    {
        // 🌟 [추가] 텔레포트 중에는 입력을 완전히 차단 (GlobalManager의 자물쇠 활용)
        if (GlobalManager.Instance != null && GlobalManager.Instance.isTeleporting)
            return;

        // 포탈 안에 내 캐릭터가 있고 + 위 방향키 또는 W 키를 눌렀을 때
        if (isInside && localPlayerInPortal != null && (Input.GetKeyDown(KeyCode.UpArrow) || Input.GetKeyDown(KeyCode.W)))
        {
            if (GlobalManager.Instance == null)
                Debug.LogError($"[SmartPortal] GlobalManager가 없습니다!");
            else if (destination == null)
                Debug.LogError($"[SmartPortal] 목적지(Destination)가 할당되지 않았습니다!");
            else if (targetMap == null)
                Debug.LogError($"[SmartPortal] 타겟 맵(Target Map)이 할당되지 않았습니다!");
            else
            {
                // 텔레포트 실행
                GlobalManager.Instance.StartTeleport(localPlayerInPortal, destination, targetMap);
                
                // 텔레포트 중복 방지
                isInside = false; 
            }
        }
    }
}