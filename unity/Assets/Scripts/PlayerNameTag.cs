using FishNet.Object;
using FishNet.Object.Synchronizing;
using TMPro;
using UnityEngine;
using UnityEngine.Rendering; // 🌟 SortingGroup을 사용하기 위해 필수입니다!

public class PlayerNameTag : NetworkBehaviour
{
    [Header("이름표 UI")]
    // 🌟 버그 방지: GetComponentInChildren 대신 인스펙터에서 직접 넣도록 public으로 변경!
    public TMP_Text nameText;
    private Transform canvasTransform;
    private Vector3 originalCanvasScale;

    // ==========================================
    // 🌟 [추가] 말풍선 UI 연결 변수
    // ==========================================
    [Header("말풍선 UI")]
    public GameObject speechBubbleObj; // SpeechBubbleCanvas 전체
    public TMP_Text bubbleText;

    // FishNet v4 방식: 동기화 변수 선언
    private readonly SyncVar<int> layerIndex = new SyncVar<int>(0);
    public readonly SyncVar<string> playerName = new SyncVar<string>("Loading...");

    // 🌟 [추가] 말풍선 내용 동기화 변수
    private readonly SyncVar<string> bubbleMessage = new SyncVar<string>("");

    public void SetLayerIndex(int index)
    {
        layerIndex.Value = index;
    }

    private void Awake()
    {
        Canvas canvas = GetComponentInChildren<Canvas>();
        if (canvas != null)
        {
            canvas.worldCamera = Camera.main;
            canvasTransform = canvas.transform;
            originalCanvasScale = canvasTransform.localScale;
        }

        layerIndex.OnChange += OnLayerIndexChanged;
        playerName.OnChange += OnNameChanged;

        // 🌟 [추가] 말풍선 동기화 이벤트 연결
        bubbleMessage.OnChange += OnBubbleMessageChanged;
    }

    private void OnLayerIndexChanged(int oldValue, int newValue, bool asServer)
    {
        SortingGroup sortingGroup = GetComponent<SortingGroup>();
        if (sortingGroup != null)
        {
            sortingGroup.sortingOrder = newValue;
        }
    }

    private void LateUpdate()
    {
        if (canvasTransform != null)
        {
            canvasTransform.position = transform.position + Vector3.up * -3.3f;
            canvasTransform.rotation = Quaternion.identity;

            float parentSign = Mathf.Sign(transform.lossyScale.x);
            canvasTransform.localScale = new Vector3(
                originalCanvasScale.x * parentSign,
                originalCanvasScale.y,
                originalCanvasScale.z
            );
        }
    }

    private void OnNameChanged(string oldValue, string newValue, bool asServer)
    {
        if (nameText != null) nameText.text = newValue;
    }

    // ==========================================
    // 🌟 [추가] 말풍선 핵심 기능 (서버 & 클라이언트 동기화)
    // ==========================================
    [Server]
    public void ShowSpeechBubble(string message)
    {
        bubbleMessage.Value = message; // 글자를 넣으면 모든 클라이언트 화면에 켜짐

        CancelInvoke(nameof(HideBubble));
        Invoke(nameof(HideBubble), 3.0f); // 3초 뒤에 삭제
    }

    private void HideBubble()
    {
        bubbleMessage.Value = ""; // 글자를 비우면 꺼짐
    }

    private void OnBubbleMessageChanged(string oldVal, string newVal, bool asServer)
    {
        if (string.IsNullOrEmpty(newVal))
        {
            if (speechBubbleObj != null) speechBubbleObj.SetActive(false);
        }
        else
        {
            if (speechBubbleObj != null) speechBubbleObj.SetActive(true);
            if (bubbleText != null) bubbleText.text = newVal;
        }
    }
    // ==========================================

    // ==========================================
    // 🌟 [수정] 닉네임 동기화 로직 (클라이언트 -> 서버)
    // ==========================================
    public override void OnStartClient()
    {
        base.OnStartClient();

        // IsOwner: 이 캐릭터가 '내 PC에서 조종하는 내 캐릭터'일 때만 실행
        if (IsOwner)
        {
            // 1. AuthManager에서 내 닉네임을 가져옵니다. (AuthManager 수정 필요 - 아래 참고)
            string myNickname = AuthManager.Instance.playerNickname;

            // 2. 닉네임이 비어있지 않다면 서버로 전송합니다.
            if (!string.IsNullOrEmpty(myNickname))
            {
                CmdSetPlayerName(myNickname);
            }
            else
            {
                // 혹시 닉네임을 못 불러왔을 때의 예외 처리
                CmdSetPlayerName("Unknown_" + Random.Range(100, 999));
            }
        }
    }

    // 🌟 [추가] 클라이언트 -> 서버로 닉네임 전송 (ServerRpc)
    // ==========================================
    [ServerRpc]
    private void CmdSetPlayerName(string name)
    {
        // 클라이언트가 보낸 닉네임을 서버가 받아서 SyncVar에 적용합니다.
        // 그러면 FishNet이 알아서 모든 유저의 화면에 이 닉네임을 띄워줍니다!
        playerName.Value = name;
    }

    public override void OnStopNetwork()
    {
        base.OnStopNetwork();
        if (canvasTransform != null) Destroy(canvasTransform.gameObject);
    }
}