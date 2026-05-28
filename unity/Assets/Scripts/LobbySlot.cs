using UnityEngine;
using TMPro;
using Steamworks.Data; // 여기에도 Image가 있고
using UnityEngine.UI;  // 여기에도 Image가 있어서 충돌 발생!

public class LobbySlot : MonoBehaviour
{
    [Header("UI Elements")]
    public TextMeshProUGUI roomInfoText;
    public Button joinButton;

    // 🌟 [수정] UnityEngine.UI를 앞에 직접 붙여서 모호성을 해결합니다.
    public UnityEngine.UI.Image lockIconImage;

    private Lobby lobbyData;

    public void Setup(Lobby lobby)
    {
        lobbyData = lobby;

        string roomName = lobby.GetData("LobbyName");
        if (string.IsNullOrWhiteSpace(roomName)) roomName = "알 수 없는 방";

        string hostName = lobby.GetData("OwnerName");
        if (string.IsNullOrWhiteSpace(hostName)) hostName = "익명";

        bool isPrivate = lobby.GetData("IsPrivate") == "true";

        int currentMembers = lobby.MemberCount;
        int maxMembers = lobby.MaxMembers;

        // 자물쇠 이미지를 비밀번호 방일 때만 활성화
        if (lockIconImage != null)
        {
            lockIconImage.gameObject.SetActive(isPrivate);
        }

        roomInfoText.text = $"<color=#FFFFFF><b>{roomName}</b></color> <size=80%>(방장: {hostName})</size>\n" +
                            $"<color=#FFCC00>참여 인원: {currentMembers} / {maxMembers}</color>";

        joinButton.onClick.RemoveAllListeners();

        if (currentMembers >= maxMembers)
        {
            joinButton.interactable = false;
            roomInfoText.text += " <color=red>[FULL]</color>";
        }
        else
        {
            joinButton.interactable = true;
            joinButton.onClick.AddListener(OnJoinClick);
        }
    }

    private void OnJoinClick()
    {
        joinButton.interactable = false;
        Debug.Log($"[LobbySlot] {lobbyData.Id}번 로비 입장 시도 (비공개 여부: {lobbyData.GetData("IsPrivate")})");

        if (SteamManager.Instance != null)
        {
            SteamManager.Instance.SelectLobbyAndJoin(lobbyData);
        }

        Invoke(nameof(ReEnableButton), 1.5f);
    }

    private void ReEnableButton()
    {
        if (joinButton != null) joinButton.interactable = true;
    }
}