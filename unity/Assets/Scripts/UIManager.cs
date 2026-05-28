using UnityEngine;
using TMPro;
using System.Text.RegularExpressions;
using Steamworks.Data;
using UnityEngine.UI;
using System.Collections;
using UnityEngine.SceneManagement;

public class UIManager : MonoBehaviour
{
    public static UIManager Instance;

    [Header("글로벌 UI 그룹 (전체 부모)")]
    public GameObject introUI;
    public GameObject mainGameUI; // Inspector에서 CanvasUi를 넣어주세요.
    public GameObject fadePanel;

    [Header("패널 오브젝트들 (IntroUI의 자식들)")]
    public GameObject modeSelectPanel;
    public GameObject createLobbyPanel;
    public GameObject lobbyListPanel;
    public GameObject quitCheckPanel;

    [Header("방 설정 UI 요소들")]
    public TMP_InputField lobbyNameInput;
    public TMP_InputField passwordInput;
    public TMP_Dropdown maxPlayersDropdown;

    [Header("비밀번호 입력 팝업")]
    public GameObject passwordCheckPanel;
    public TMP_InputField passwordInputField;
    public TMP_Text passwordErrorText;

    private Lobby _pendingLobby;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            // 캔버스 루트 오브젝트를 파괴하지 않음 (UIManager가 포함된 부모 Canvas)
            DontDestroyOnLoad(transform.root.gameObject);
        }
        else
        {
            Destroy(transform.root.gameObject);
            return;
        }
    }
    private void Update()
    {
        // 1. ESC 키 입력 감지
        if (Input.GetKeyDown(KeyCode.Escape))
        {
            ToggleQuitPanel();
        }
    }

    // 2. 패널을 끄고 켜는 토글 함수
    public void ToggleQuitPanel()
    {
        if (quitCheckPanel == null) return;

        // 현재 상태의 반대로 설정 (켜져 있으면 끄고, 꺼져 있으면 킴)
        bool isActive = quitCheckPanel.activeSelf;
        quitCheckPanel.SetActive(!isActive);

        // [선택 사항] 패널이 켜질 때 마우스 커서 설정 등을 추가할 수 있습니다.
        if (!isActive)
        {
            Debug.Log("<color=white>[UI] 종료 확인 패널 활성화 (ESC)</color>");
        }
    }

    // 기존의 버튼 연결 함수도 이 토글 함수를 쓰도록 바꾸면 깔끔합니다.
    public void OnClickGameQuitButton()
    {
        ToggleQuitPanel();
    }



    private void OnEnable() { SceneManager.sceneLoaded += OnSceneLoaded; }
    private void OnDisable() { SceneManager.sceneLoaded -= OnSceneLoaded; }

    private void OnSceneLoaded(Scene scene, LoadSceneMode mode)
    {
        Debug.Log($"<color=yellow>[UI 체크] 현재 로드된 씬 이름: '{scene.name}'</color>");

        // 🌟 [중요] 모든 씬 이동 시 일단 화면을 즉시 검정색으로 덮습니다.
        if (fadePanel != null)
        {
            fadePanel.SetActive(true);
            CanvasGroup cg = fadePanel.GetComponent<CanvasGroup>();
            if (cg == null) cg = fadePanel.AddComponent<CanvasGroup>();
            cg.alpha = 1f;
        }

        if (scene.name.Trim().Equals("GameStart", System.StringComparison.OrdinalIgnoreCase))
        {
            if (introUI != null) introUI.SetActive(true);
            if (mainGameUI != null) mainGameUI.SetActive(false);
            CloseAllPanels();

            // 인트로(타이틀) 씬에서만 UIManager가 직접 페이드를 걷어냅니다.
            StartCoroutine(DisableFadeAfterDelay(0.5f));
            RebindIntroButtons();
        }
        else if (scene.name.Trim().Equals("MainScene", System.StringComparison.OrdinalIgnoreCase))
        {
            if (introUI != null) introUI.SetActive(false);

            if (mainGameUI != null)
            {
                mainGameUI.SetActive(true);
                // UI 자식들 활성화
                for (int i = 0; i < mainGameUI.transform.childCount; i++)
                {
                    mainGameUI.transform.GetChild(i).gameObject.SetActive(true);
                }

                Canvas canvas = mainGameUI.GetComponentInParent<Canvas>();
                if (canvas != null) canvas.sortingOrder = 999;
            }

            // ⚠️ [수정 핵심] MainScene 로드 시 DisableFadeAfterDelay를 여기서 호출하지 않습니다!
            // 이제 GlobalManager가 캐릭터 스폰/카메라 세팅 완료 후 직접 끌 것입니다.
            Debug.Log("<color=cyan>[UI] MainScene 로드됨. 페이드 제어권을 GlobalManager로 넘깁니다.</color>");
        }
    }

    // 페이드를 부드럽게 제거하는 루틴 (인트로 전용)
    public IEnumerator DisableFadeAfterDelay(float delay)
    {
        yield return new WaitForSeconds(delay);
        if (fadePanel != null)
        {
            CanvasGroup cg = fadePanel.GetComponent<CanvasGroup>();
            if (cg != null)
            {
                float duration = 0.5f;
                float time = 0;
                while (time < duration)
                {
                    time += Time.deltaTime;
                    cg.alpha = Mathf.Lerp(1f, 0f, time / duration);
                    yield return null;
                }
                cg.alpha = 0f;
            }
            fadePanel.SetActive(false);
            Debug.Log("<color=cyan>[UI] 페이드 비활성화 완료</color>");
        }
    }

    private void RebindIntroButtons()
    {
        GameObject quitBtnObj = GameObject.Find("QuitButton");
        if (quitBtnObj != null)
        {
            Button btn = quitBtnObj.GetComponent<Button>();
            btn.onClick.RemoveAllListeners();
            btn.onClick.AddListener(OnClickGameQuitButton);
        }
    }

    // --- 패널 제어 로직 ---
    public void OpenModeSelect() { CloseAllPanels(); if (modeSelectPanel != null) modeSelectPanel.SetActive(true); }
    public void OpenCreateLobby() { CloseAllPanels(); if (createLobbyPanel != null) createLobbyPanel.SetActive(true); }
    public void BackToModeSelect() { if (createLobbyPanel != null) createLobbyPanel.SetActive(false); if (modeSelectPanel != null) modeSelectPanel.SetActive(true); }
    public void OpenLobbyList() { CloseAllPanels(); if (lobbyListPanel != null) { lobbyListPanel.SetActive(true); if (SteamManager.Instance != null) SteamManager.Instance.RefreshLobbyList(); } }
    public void BackFromLobbyList() { if (lobbyListPanel != null) lobbyListPanel.SetActive(false); if (modeSelectPanel != null) modeSelectPanel.SetActive(true); }

    public void ConfirmCreateLobby()
    {
        // 방 만들기 버튼 클릭 시 즉시 암전
        /*
        if (fadePanel != null)
        {
            fadePanel.SetActive(true);
            CanvasGroup cg = fadePanel.GetComponent<CanvasGroup>();
            if (cg == null) cg = fadePanel.AddComponent<CanvasGroup>();
            cg.alpha = 1f;
        }
        */

        string lobbyName = lobbyNameInput.text;
        string password = passwordInput.text;

        if (string.IsNullOrWhiteSpace(lobbyName)) lobbyName = "알 수 없는 방";
        else if (lobbyName.Length > 15) lobbyName = lobbyName.Substring(0, 15);

        string selectedText = maxPlayersDropdown.options[maxPlayersDropdown.value].text;
        string numberOnly = Regex.Replace(selectedText, @"[^0-9]", "");
        int maxPlayers = 4;
        if (int.TryParse(numberOnly, out int result)) maxPlayers = result;

        if (SteamManager.Instance != null)
            SteamManager.Instance.CreateSteamLobby(lobbyName, password, maxPlayers);
    }

    public void GoToIntroScene()
    {
        if (quitCheckPanel != null) quitCheckPanel.SetActive(false);

        // 🌟 [수정] 즉시 암전(1f) 대신 부드러운 코루틴 호출
        if (fadePanel != null)
        {
            // 0.5초 동안 서서히 어두워지게 함
            StartCoroutine(FadeToBlackAndLeave(0.5f));
        }
    }

    // 🌟 [추가] 화면이 완전히 어두워진 후 씬을 이동시키는 헬퍼 코루틴
    private IEnumerator FadeToBlackAndLeave(float duration)
    {
        if (fadePanel != null)
        {
            fadePanel.SetActive(true);
            CanvasGroup cg = fadePanel.GetComponent<CanvasGroup>();
            if (cg == null) cg = fadePanel.AddComponent<CanvasGroup>();

            float startAlpha = cg.alpha;
            float time = 0;

            // 서서히 어두워짐
            while (time < duration)
            {
                time += Time.deltaTime;
                cg.alpha = Mathf.Lerp(startAlpha, 1f, time / duration);
                yield return null;
            }
            cg.alpha = 1f;
        }

        // 화면이 완전히 깜깜해진 후 연결 끊고 이동
        if (SteamManager.Instance != null)
            SteamManager.Instance.DisconnectAndGoToIntro();
        else
            SceneManager.LoadScene("GameStart");
    }

    public void RealQuitGame()
    {
#if UNITY_EDITOR
        UnityEditor.EditorApplication.isPlaying = false;
#else
        Application.Quit();
#endif
    }

    public void CloseQuitPopup() { if (quitCheckPanel != null) quitCheckPanel.SetActive(false); }
    public void OpenPasswordCheckPopup(Lobby lobby) { _pendingLobby = lobby; passwordInputField.text = ""; if (passwordErrorText != null) passwordErrorText.gameObject.SetActive(false); if (passwordCheckPanel != null) passwordCheckPanel.SetActive(true); }
    public void OnClickPasswordConfirm()
    {
        if (passwordInputField.text == _pendingLobby.GetData("LobbyPassword"))
        {
            ClosePasswordCheckPopup();
            if (SteamManager.Instance != null) SteamManager.Instance.JoinLobbyDirect(_pendingLobby);
        }
        else if (passwordErrorText != null)
        {
            passwordErrorText.text = "비밀번호가 틀렸습니다.";
            passwordErrorText.gameObject.SetActive(true);
        }
    }
    public void ClosePasswordCheckPopup() { if (passwordCheckPanel != null) passwordCheckPanel.SetActive(false); }
    public void CloseAllPanels()
    {
        if (createLobbyPanel != null) createLobbyPanel.SetActive(false);
        if (modeSelectPanel != null) modeSelectPanel.SetActive(false);
        if (lobbyListPanel != null) lobbyListPanel.SetActive(false);
        if (passwordCheckPanel != null) passwordCheckPanel.SetActive(false);
        if (quitCheckPanel != null) quitCheckPanel.SetActive(false);
    }
}