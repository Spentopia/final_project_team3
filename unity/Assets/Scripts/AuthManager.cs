using UnityEngine;
using UnityEngine.Networking;
using TMPro;
using UnityEngine.UI;
using System.Collections;
using System;

[System.Serializable]
public class AuthResponse
{
    public string access_token;
    public string refresh_token;
    public string nickname;
}

public class AuthManager : MonoBehaviour
{
    public static AuthManager Instance;

    [Header("UI Panels")]
    public GameObject loginPanel;      // 전체 로그인 패널 (IntroUI 등)
    public TextMeshProUGUI authCompleteUI;  // "인증이 완료되었습니다" 텍스트/오브젝트
    public GameObject gameStartBtn;    // "게임 시작" 버튼

    [Header("UI Inputs & Texts")]
    public TMP_InputField codeInputField;
    public TextMeshProUGUI timerText;

    [Header("Auth Settings")]
    public string verifyApiUrl = "https://api.spentopia.net/auth/handoff/exchange";
    public float limitTime = 60f; // 1분 제한

    [HideInInspector] public string accessToken;
    [HideInInspector] public string playerNickname;

    private bool isVerified = false;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            // 부모가 이미 보호해주고 있으므로 DontDestroyOnLoad는 지웁니다!
        }
        else
        {
            Destroy(gameObject);
        }
    }

    private void Start()
    {
        // 1. 초기 UI 상태 세팅 (성공 UI와 버튼은 숨김)
        authCompleteUI.gameObject.SetActive(false);
        gameStartBtn.SetActive(false);
        loginPanel.SetActive(true);

        // 2. 타이머 시작
        StartCoroutine(AuthTimerCoroutine());
    }

    // [인증하기] 버튼 클릭 시 실행
    public void OnClickVerify()
    {
        if (isVerified) return;
        StartCoroutine(VerifyCodeRoutine(codeInputField.text));
    }

    private IEnumerator VerifyCodeRoutine(string inputCode)
    {
        string jsonBody = "{\"auth_code\":\"" + inputCode + "\"}";

        using (UnityWebRequest webRequest = new UnityWebRequest(verifyApiUrl, "POST"))
        {
            byte[] bodyRaw = System.Text.Encoding.UTF8.GetBytes(jsonBody);
            webRequest.uploadHandler = new UploadHandlerRaw(bodyRaw);
            webRequest.downloadHandler = new DownloadHandlerBuffer();
            webRequest.SetRequestHeader("Content-Type", "application/json");

            yield return webRequest.SendWebRequest();

            if (webRequest.result == UnityWebRequest.Result.Success)
            {
                AuthResponse data = JsonUtility.FromJson<AuthResponse>(webRequest.downloadHandler.text);
                accessToken = data.access_token;
                playerNickname = data.nickname;

                // 인증 성공 처리 함수 호출
                OnAuthSuccess();
            }
            else
            {
                Debug.LogError("❌ 인증 실패: " + webRequest.error);

                // ✏️ [추가] 인증 번호가 틀렸을 때 처리
                OnAuthFailed();
            }
        }
    }

    private void OnAuthSuccess()
    {
        isVerified = true;
        StopAllCoroutines();

        // 📦 인벤토리 데이터 즉시 요청
        if (InventoryManager.Instance != null)
        {
            Debug.Log("📦 인증 성공! 인벤토리 데이터를 불러옵니다...");
            InventoryManager.Instance.LoadInventoryFromServer(accessToken);
        }
        else
        {
            Debug.LogWarning("⚠️ InventoryManager를 찾을 수 없습니다!");
        }

        // ✏️ [수정] 성공 텍스트 및 색상 세팅 (초록색 혹은 흰색 권장)
        authCompleteUI.text = "인증이 완료되었습니다";
        authCompleteUI.color = Color.green; // 혹은 원래 쓰시던 색상 Color.white;
        authCompleteUI.gameObject.SetActive(true);

        gameStartBtn.SetActive(true);

        timerText.gameObject.SetActive(false);
        codeInputField.interactable = false;

        Debug.Log($"✅ {playerNickname}님 로그인 성공!");
    }

    // ✏️ [추가] 인증 실패 시 작동할 함수
    private void OnAuthFailed()
    {
        authCompleteUI.text = "인증번호가 틀렸습니다";
        authCompleteUI.color = Color.red; // 빨간색 글씨로 변경
        authCompleteUI.gameObject.SetActive(true); // 텍스트 띄우기

        // 틀렸을 땐 입력창을 다시 깔끔하게 비워주고 포커스를 주면 UX에 좋습니다.
        codeInputField.text = "";
        codeInputField.ActivateInputField();
    }

    // 🕒 1분 카운트다운 코루틴
    IEnumerator AuthTimerCoroutine()
    {
        float remainingTime = limitTime;

        while (remainingTime > 0)
        {
            if (isVerified) yield break; // 인증 성공 시 카운트다운 즉시 탈출

            remainingTime -= Time.deltaTime;

            // UI에 남은 시간 표시
            timerText.text = $"남은 시간 : {Mathf.CeilToInt(remainingTime)}초";

            // 💀 시간이 다 되면 게임 자동 종료
            if (remainingTime <= 0)
            {
                timerText.text = "남은 시간 : 0초";
                Debug.LogError("⏰ 시간 초과! 게임을 종료합니다.");

                yield return new WaitForSeconds(0.5f); // 0초를 보여주기 위한 짧은 대기

                ExitGame();
            }
            yield return null;
        }
    }

    // 게임 종료 로직 (에디터/빌드 환경 대응)
    private void ExitGame()
    {
#if UNITY_EDITOR
        UnityEditor.EditorApplication.isPlaying = false;
#else
            Application.Quit();
#endif
    }

    // [GameStart_Btn] 클릭 시 실행
    // [GameStart_Btn] 또는 [GameEntrance_Btn]에 이 함수를 연결하세요
    public void OnClickGameStart()
    {
        // 1. 로그인 전체 창(IntroUI)을 화면에서 사라지게 만듭니다.
        if (loginPanel != null)
        {
            loginPanel.SetActive(false);
            // 만약 다시는 이 창이 필요 없다면 아래처럼 아예 파괴해도 됩니다.
            // Destroy(loginPanel); 
        }

        Debug.Log("🚀 인증 창이 사라지고 게임 월드로 진입합니다.");

        // 2. 만약 씬 이동이 필요하다면 여기에 추가하세요.
        // SceneManager.LoadScene("MainGameScene"); 
    }
}