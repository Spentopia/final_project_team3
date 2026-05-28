using UnityEngine;
using TMPro;
using FishNet.Object;
using FishNet.Connection;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using System.Collections;

public class GlobalChatManager : NetworkBehaviour
{
    [Header("스크롤 설정")]
    public ScrollRect chatScrollRect;

    [Header("UI 연결")]
    public TMP_InputField chatInput;
    public TextMeshProUGUI chatHistoryText;

    // ==========================================
    // 🌟 [수정됨] 채팅창 전체 배경 패널 조절 설정
    // ==========================================
    [Header("채팅창 크기 조절")]
    // 💡 중요: 인스펙터에서 반투명 배경 패널(Panel)을 여기에 연결하세요!
    public RectTransform chatBackgroundPanel; 
    
    public float largeHeight = 350f; // 평소 (큰) 높이
    public float smallHeight = 150f; // 줄였을 때 (작은) 높이
    private bool isChatSmall = false; 

    public static bool IsChatFocused { get; private set; }

    private void Start()
    {
        if (chatHistoryText != null) chatHistoryText.text = "";
    }

    private void Update()
    {
        if (chatInput != null)
        {
            IsChatFocused = chatInput.isFocused;
        }

        // 엔터키를 눌렀을 때
        if (Input.GetKeyDown(KeyCode.Return) || Input.GetKeyDown(KeyCode.KeypadEnter))
        {
            if (chatInput == null) return;

            // 🌟 [핵심 수정] 현재 유니티 마우스/키보드 포커스가 '채팅 입력창'에 있는지 확실하게 검사
            bool isCurrentlySelectingInput = EventSystem.current != null &&
                                            EventSystem.current.currentSelectedGameObject == chatInput.gameObject;

            // 1. 채팅창을 안 보고 있다면 -> 활성화하고 포커스 주기
            if (!isCurrentlySelectingInput)
            {
                chatInput.ActivateInputField();
                chatInput.Select();
                // 팁: 인풋 필드 뒤에 커서가 가도록 강제 조절
                chatInput.MoveTextEnd(false);
            }
            // 2. 이미 채팅창을 보고 있는 상태에서 엔터를 누른 거라면 -> 전송 후 닫기
            else
            {
                // 글자가 들어있다면 전송
                if (!string.IsNullOrWhiteSpace(chatInput.text))
                {
                    SendChatMessage();
                    chatInput.text = ""; // 인풋 필드 비우기
                }

                // 포커스 해제하여 캐릭터 조작으로 복귀
                chatInput.DeactivateInputField();
                if (EventSystem.current != null) EventSystem.current.SetSelectedGameObject(null);
            }
        }
    }

    private void SendChatMessage()
    {
        Debug.Log("[Chat] SendChatMessage 호출됨!");
        RequestSendChatServerRpc(chatInput.text);
    }

    [ServerRpc(RequireOwnership = false)]
    private void RequestSendChatServerRpc(string message, NetworkConnection caller = null)
    {
        string senderName = $"Player_{caller.ClientId}";

        if (caller != null && caller.FirstObject != null)
        {
            PlayerNameTag nameTag = caller.FirstObject.GetComponent<PlayerNameTag>();
            if (nameTag != null)
            {
                senderName = nameTag.playerName.Value;
                nameTag.ShowSpeechBubble(message);
            }
        }

        string formattedMessage = $"[{senderName}]: {message}";
        BroadcastChatObserversRpc(formattedMessage);
    }

    [ObserversRpc]
    private void BroadcastChatObserversRpc(string formattedMessage)
    {
        chatHistoryText.text += "\n" + formattedMessage;
        StartCoroutine(ScrollToBottom());
    }

    private IEnumerator ScrollToBottom()
    {
        yield return null;

        if (chatScrollRect != null && chatScrollRect.content != null)
        {
            Canvas.ForceUpdateCanvases();
            UnityEngine.UI.LayoutRebuilder.ForceRebuildLayoutImmediate(chatScrollRect.content);
        }

        yield return new WaitForEndOfFrame();

        if (chatScrollRect != null)
        {
            chatScrollRect.verticalNormalizedPosition = 0f;
        }
    }

    // ==========================================
    // 🌟 [수정됨] 전체 패널 높이를 조절하는 함수
    // ==========================================
    public void ToggleChatSize()
    {
        if (chatBackgroundPanel != null)
        {
            isChatSmall = !isChatSmall;

            // 배경 패널의 높이를 직접 바꿉니다. 
            // (Pivot Y가 0이어야 아래를 기준으로 위가 줄어듭니다!)
            chatBackgroundPanel.sizeDelta = new Vector2(
                chatBackgroundPanel.sizeDelta.x, 
                isChatSmall ? smallHeight : largeHeight
            );

            // 크기 변경 후 스크롤 위치 재조정
            StartCoroutine(ScrollToBottom());
        }
    }
}