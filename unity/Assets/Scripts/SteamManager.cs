using UnityEngine;
using Steamworks;
using Steamworks.Data;
using System.Threading.Tasks;
using System.Collections;
using FishNet;
using FishNet.Managing.Scened;
using FishNet.Transporting; // 추가
using FishyFacepunch;
using UnityEngine.SceneManagement;

public class SteamManager : MonoBehaviour
{
    public static SteamManager Instance;

    [Header("UI References")]
    public GameObject lobbySlotPrefab;
    public Transform contentParent;

    public Lobby? CurrentLobby { get; private set; }
    private bool _isHosting = false;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }
        else
        {
            Destroy(gameObject);
            return;
        }

#if !UNITY_EDITOR
        try
        {
            if (SteamClient.RestartAppIfNecessary(480))
            {
                Debug.Log("<color=orange>[Steam] 스팀을 통해 게임을 재시작합니다.</color>");
                Application.Quit();
                return;
            }
        }
        catch (System.Exception e)
        {
            Debug.LogError($"[Steam] RestartAppIfNecessary 예외: {e.Message}");
        }
#endif
    }


    private async void Start()
    {
#if !UNITY_EDITOR
        // --- 스팀 이벤트 연결 ---
        SteamMatchmaking.OnLobbyEntered += OnLobbyEnteredCallback;
        SteamFriends.OnGameOverlayActivated += OnOverlayStatusChanged;
        SteamMatchmaking.OnLobbyMemberLeave += OnMemberLeaveCallback;
        SteamMatchmaking.OnLobbyMemberDisconnected += OnMemberLeaveCallback;

        // 🌟 [추가] FishNet 네트워크 연결 상태 감지 이벤트 등록
        // 방장이 방을 나가서 서버가 닫히면 참가자의 클라이언트 연결이 끊기는 것을 감지합니다.
        InstanceFinder.ClientManager.OnClientConnectionState += OnClientConnectionStateChange;

        // [오버레이 참가] 스팀 초대 수락 시 암전 후 입장
        SteamFriends.OnGameLobbyJoinRequested += (lobby, steamId) =>
        {
            Debug.Log($"<color=orange>[Steam] 초대 수락 감지! 로비 ID: {lobby.Id}</color>");
            StartCoroutine(JoinLobbyWithFade(lobby)); 
        };

        // --- 스팀 초기화 로직 ---
        int retryCount = 0;
        while (!SteamClient.IsValid && retryCount < 3)
        {
            try { SteamClient.Init(480); }
            catch (System.Exception e) { Debug.LogError($"[Steam] 초기화 에러: {e.Message}"); }

            if (SteamClient.IsValid) break;
            retryCount++;
            await Task.Delay(1000);
        }

        if (SteamClient.IsValid)
            Debug.Log($"<color=green>[Steam] 초기화 성공! 사용자: {SteamClient.Name}</color>");
#else
        Debug.Log("<color=cyan>[Steam] 에디터 모드입니다. 스팀 기능을 비활성화합니다.</color>");
        await Task.CompletedTask;
#endif
    }

    private void Update()
    {
#if !UNITY_EDITOR
        if (!SteamClient.IsValid) return;
        SteamClient.RunCallbacks();

        if (GlobalChatManager.IsChatFocused) return;

        if (Input.GetKeyDown(KeyCode.F3))
        {
            OpenInviteDialog();
        }
#endif
    }

    // 🌟 [네트워크 단절 감지] 서버(방장)와의 연결이 끊겼을 때 호출
    private void OnClientConnectionStateChange(ClientConnectionStateArgs args)
    {
        // 연결이 완전히 종료되었을 때
        if (args.ConnectionState == LocalConnectionState.Stopped)
        {
            // 내가 방장이 아닌데(참가자인데) 연결이 끊겼다면, 방장이 나간 것으로 간주
            if (!_isHosting)
            {
                Debug.Log("<color=red>[Network] 서버와의 연결이 끊겼습니다. 인트로로 이동합니다.</color>");
                DisconnectAndGoToIntro();
            }
        }
    }

    private void OnMemberLeaveCallback(Lobby lobby, Friend friend)
    {
        if (friend.Id == lobby.Owner.Id)
        {
            Debug.Log("<color=red>[Steam] 방장이 로비를 떠났습니다. 메인 메뉴로 귀환합니다.</color>");
            DisconnectAndGoToIntro();
        }
    }

    private void OnOverlayStatusChanged(bool active)
    {
        Debug.Log($"[Steam] 오버레이 상태: {(active ? "ON" : "OFF")}");
    }

    public async void CreateSteamLobby(string lobbyName, string password, int maxPlayers)
    {
        if (!SteamClient.IsValid) return;
        if (string.IsNullOrWhiteSpace(lobbyName)) lobbyName = "알 수 없는 방";
        string hostName = SteamClient.Name;
        _isHosting = true;

        if (UIManager.Instance != null) StartCoroutine(FadeInRoutine(0.8f));

        await Task.Delay(800);

        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
        {
            UIManager.Instance.fadePanel.SetActive(true);
            var cg = UIManager.Instance.fadePanel.GetComponent<CanvasGroup>();
            if (cg != null) cg.alpha = 1f;
        }

        if (InstanceFinder.NetworkManager.TransportManager.Transport is FishyFacepunch.FishyFacepunch transport)
        {
            transport.SetPort(15);
        }

        InstanceFinder.ServerManager.StartConnection();
        InstanceFinder.ClientManager.StartConnection();

        await Task.Delay(500);

        var lobbyOutput = await SteamMatchmaking.CreateLobbyAsync(maxPlayers);
        if (lobbyOutput.HasValue)
        {
            CurrentLobby = lobbyOutput.Value;
            CurrentLobby.Value.SetPublic();
            CurrentLobby.Value.SetJoinable(true);
            CurrentLobby.Value.SetData("ProjectName", "MyFirstGame_LTH");
            CurrentLobby.Value.SetData("LobbyName", lobbyName);
            CurrentLobby.Value.SetData("OwnerName", hostName);
            CurrentLobby.Value.SetData("IsPrivate", string.IsNullOrEmpty(password) ? "false" : "true");
            if (!string.IsNullOrEmpty(password)) CurrentLobby.Value.SetData("LobbyPassword", password);

            InstanceFinder.SceneManager.LoadGlobalScenes(new SceneLoadData("MainScene"));
        }
        else
        {
            _isHosting = false;
            if (UIManager.Instance != null) StartCoroutine(UIManager.Instance.DisableFadeAfterDelay(0f));
        }
    }

    private IEnumerator FadeInRoutine(float duration)
    {
        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
        {
            UIManager.Instance.fadePanel.SetActive(true);
            CanvasGroup cg = UIManager.Instance.fadePanel.GetComponent<CanvasGroup>();
            if (cg == null) cg = UIManager.Instance.fadePanel.AddComponent<CanvasGroup>();

            float startAlpha = cg.alpha;
            float time = 0;
            while (time < duration)
            {
                time += Time.deltaTime;
                cg.alpha = Mathf.Lerp(startAlpha, 1f, time / duration);
                yield return null;
            }
            cg.alpha = 1f;
        }
    }

    public void SelectLobbyAndJoin(Lobby lobby)
    {
        if (lobby.GetData("IsPrivate") == "true")
        {
            if (UIManager.Instance != null) UIManager.Instance.OpenPasswordCheckPopup(lobby);
        }
        else
        {
            JoinLobbyDirect(lobby);
        }
    }

    public void JoinLobbyDirect(Lobby lobby)
    {
        _isHosting = false;
        if (UIManager.Instance != null) StartCoroutine(FadeInRoutine(0.8f));
        lobby.Join();
        Debug.Log($"<color=yellow>[Steam] 로비 입장 시도: {lobby.Id}</color>");
    }

    private void OnLobbyEnteredCallback(Lobby lobby)
    {
        if (_isHosting) return;
        CurrentLobby = lobby;

        if (InstanceFinder.NetworkManager.TransportManager.Transport is FishyFacepunch.FishyFacepunch transport)
        {
            transport.SetPort(15);
            transport.SetClientAddress(lobby.Owner.Id.ToString());
        }
        StartCoroutine(DelayedClientConnect());
    }

    private IEnumerator DelayedClientConnect()
    {
        yield return new WaitForSeconds(0.8f);
        InstanceFinder.ClientManager.StartConnection();
    }

    public void DisconnectAndGoToIntro()
    {
        Debug.Log("<color=red>[Steam] 모든 연결을 종료하고 메인 메뉴로 이동합니다.</color>");
        LeaveLobby();

        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
            UIManager.Instance.fadePanel.SetActive(false);

        UnityEngine.SceneManagement.SceneManager.LoadScene("GameStart");
    }

    public void OpenInviteDialog()
    {
        if (Application.isEditor) return;
        if (CurrentLobby.HasValue && SteamUtils.IsOverlayEnabled)
        {
            SteamFriends.OpenGameInviteOverlay(CurrentLobby.Value.Id);
        }
    }

    public async void RefreshLobbyList()
    {
        if (!SteamClient.IsValid) return;
        if (contentParent == null) return;
        foreach (Transform child in contentParent) Destroy(child.gameObject);

        try
        {
            var lobbies = await SteamMatchmaking.LobbyList
                .WithMaxResults(20)
                .WithKeyValue("ProjectName", "MyFirstGame_LTH")
                .RequestAsync();

            if (lobbies != null)
            {
                foreach (var lobby in lobbies)
                {
                    if (lobby.Owner.Id == SteamClient.SteamId) continue;
                    GameObject go = Instantiate(lobbySlotPrefab, contentParent);
                    go.GetComponent<LobbySlot>()?.Setup(lobby);
                }
            }
        }
        catch (System.Exception e)
        {
            Debug.LogError($"[Steam] 로비 갱신 실패: {e.Message}");
        }
    }

    public void LeaveLobby()
    {
        if (CurrentLobby.HasValue)
        {
            CurrentLobby.Value.Leave();
            CurrentLobby = null;
        }

        if (InstanceFinder.ServerManager != null && InstanceFinder.ServerManager.Started)
            InstanceFinder.ServerManager.StopConnection(true);
        else if (InstanceFinder.ClientManager != null && InstanceFinder.ClientManager.Started)
            InstanceFinder.ClientManager.StopConnection();

        _isHosting = false;
    }

    private void OnDisable() { if (Instance == this) ShutdownSteam(); }
    private void OnApplicationQuit() { if (Instance == this) ShutdownSteam(); }
    private void OnDestroy()
    {
        // 🌟 이벤트 해제 (OnClientConnectionState 포함)
        if (InstanceFinder.ClientManager != null)
            InstanceFinder.ClientManager.OnClientConnectionState -= OnClientConnectionStateChange;

        SteamMatchmaking.OnLobbyEntered -= OnLobbyEnteredCallback;
        SteamFriends.OnGameOverlayActivated -= OnOverlayStatusChanged;
        SteamMatchmaking.OnLobbyMemberLeave -= OnMemberLeaveCallback;
        SteamMatchmaking.OnLobbyMemberDisconnected -= OnMemberLeaveCallback;

        if (Instance == this) ShutdownSteam();
    }

    private void ShutdownSteam()
    {
        if (SteamClient.IsValid)
        {
            try { SteamClient.Shutdown(); }
            catch (System.Exception e) { Debug.LogError($"[Steam] Shutdown 중 오류: {e.Message}"); }
        }
    }

    private IEnumerator JoinLobbyWithFade(Lobby lobby)
    {
        yield return StartCoroutine(FadeInRoutine(0.8f));
        lobby.Join();
    }
}