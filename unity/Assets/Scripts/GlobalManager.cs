using UnityEngine;
using UnityEngine.SceneManagement;
using Unity.Cinemachine;
using System.Collections;
using FishNet;
using FishNet.Object;

public class GlobalManager : MonoBehaviour
{
    public static GlobalManager Instance;

    [Header("카메라 설정")]
    public CinemachineCamera virtualCamera;
    public CinemachineConfiner2D confiner;

    [Header("음악 설정")]
    public AudioClip[] bgmClips;
    private AudioSource bgmSource;

    [Header("UI 설정")]
    [SerializeField] private GameObject introUI;

    [Header("네트워크 UI 설정")]
    public GameObject chatManagerPrefab;

    [HideInInspector] public bool isTeleporting = false;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject);
            SceneManager.sceneLoaded += OnSceneLoaded;
            bgmSource = gameObject.AddComponent<AudioSource>();
            bgmSource.loop = true;
        }
        else Destroy(gameObject);
    }

    private void Start() { PlayMusic(0); }

    private void OnSceneLoaded(Scene scene, LoadSceneMode mode)
    {
        if (scene.name == "MainScene")
        {
            if (introUI != null) introUI.SetActive(false);
            Debug.Log("<color=cyan>[디버그] MainScene 로드됨. 초기화 코루틴 시작</color>");
            StartCoroutine(InitializeEverythingDelayed());

            Scene gameStartScene = SceneManager.GetSceneByName("GameStart");
            if (gameStartScene.isLoaded) SceneManager.UnloadSceneAsync("GameStart");
        }
    }

    // 🌟 페이드 디버깅 함수
    private IEnumerator FadeCanvasGroup(GameObject panel, float targetAlpha, float duration)
    {
        if (panel == null)
        {
            Debug.LogError("<color=red>[디버그] 페이드 실패: panel(fadePanel)이 null입니다! UIManager 인스펙터를 확인하세요.</color>");
            yield break;
        }

        CanvasGroup cg = panel.GetComponent<CanvasGroup>();
        if (cg == null)
        {
            Debug.LogWarning("<color=yellow>[디버그] CanvasGroup이 없어 자동으로 추가합니다.</color>");
            cg = panel.AddComponent<CanvasGroup>();
        }

        panel.SetActive(true);
        float startAlpha = cg.alpha;
        float time = 0;

        Debug.Log($"<color=orange>[디버그] 페이드 루틴 시작: {startAlpha} -> {targetAlpha} (시간: {duration}s)</color>");

        while (time < duration)
        {
            time += Time.deltaTime;
            cg.alpha = Mathf.Lerp(startAlpha, targetAlpha, time / duration);
            yield return null;
        }

        cg.alpha = targetAlpha;
        if (targetAlpha <= 0) panel.SetActive(false);

        Debug.Log($"<color=orange>[디버그] 페이드 루틴 완료: 최종 알파 {cg.alpha}, 패널 활성화 상태: {panel.activeSelf}</color>");
    }

    private IEnumerator InitializeEverythingDelayed()
    {
        // 🌟 [암전 강제 시작] 씬 진입 시 화면 가리기
        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
        {
            Debug.Log("<color=white>[디버그] 씬 진입 암전 세팅 시도</color>");
            UIManager.Instance.fadePanel.SetActive(true);
            var cg = UIManager.Instance.fadePanel.GetComponent<CanvasGroup>();
            if (cg != null) cg.alpha = 1f;
        }
        else
        {
            Debug.LogError("<color=red>[디버그] UIManager.Instance 또는 fadePanel을 찾을 수 없습니다!</color>");
        }

        Debug.Log("[GlobalManager] 멀티플레이어 초기화 대기 시작...");
        float networkWaitTimer = 0f;
        while (networkWaitTimer < 3.0f)
        {
            if (InstanceFinder.IsServerStarted || InstanceFinder.ClientManager.Connection.IsAuthenticated) break;
            yield return new WaitForSeconds(0.1f);
            networkWaitTimer += 0.1f;
        }

        yield return new WaitForSeconds(0.3f);

        if (InstanceFinder.IsServerStarted && chatManagerPrefab != null)
        {
            if (FindFirstObjectByType<GlobalChatManager>() == null)
            {
                GameObject chatObj = Instantiate(chatManagerPrefab);
                InstanceFinder.ServerManager.Spawn(chatObj);
            }
        }

        GameObject localPlayer = null;
        float timer = 0f;
        while (localPlayer == null && timer < 7.0f)
        {
            NetworkObject[] netObjs = Object.FindObjectsByType<NetworkObject>(FindObjectsInactive.Include, FindObjectsSortMode.None);
            foreach (var nob in netObjs)
            {
                if (nob.IsOwner && nob.GetComponent<PlayerNameTag>() != null)
                {
                    localPlayer = nob.gameObject;
                    break;
                }
            }
            if (localPlayer == null) { yield return new WaitForSeconds(0.2f); timer += 0.2f; }
        }

        // --- 카메라 및 플레이어 세팅 (원복된 로직 그대로 유지) ---
        if (localPlayer != null)
        {
            Rigidbody2D rb = localPlayer.GetComponent<Rigidbody2D>();
            if (rb != null) rb.simulated = false;

            GameObject spawnPoint = GameObject.Find("SpawnPoint_Start");
            MapInfo startMap = null;

            if (spawnPoint != null)
            {
                localPlayer.transform.position = spawnPoint.transform.position;
                startMap = spawnPoint.GetComponentInParent<MapInfo>();
            }

            if (startMap == null)
            {
                MapInfo[] allMaps = FindObjectsByType<MapInfo>(FindObjectsInactive.Exclude, FindObjectsSortMode.None);
                if (allMaps.Length > 0) startMap = allMaps[0];
            }

            ForceFindReferences();
            SetCameraTarget(localPlayer.transform);

            yield return new WaitForEndOfFrame();
            if (startMap != null) ApplyMapSettings(startMap.gameObject);
            if (confiner != null && confiner.enabled) confiner.InvalidateBoundingShapeCache();
            if (rb != null) rb.simulated = true;
            Debug.Log("<color=green>[디버그] 플레이어/카메라 세팅 완료</color>");
        }

        // 🌟 [암전 해제] 세팅 끝났으니 페이드 아웃
        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
        {
            Debug.Log("<color=white>[디버그] 세팅 완료. 밝아지기 시작.</color>");
            yield return StartCoroutine(FadeCanvasGroup(UIManager.Instance.fadePanel, 0f, 1.0f));
        }
    }

    public void StartTeleport(GameObject player, Transform dest, GameObject targetMap)
    {
        Debug.Log("<color=magenta>[디버그] 텔레포트(포탈) 실행</color>");
        StartCoroutine(TeleportRoutine(player, dest, targetMap));
    }

    private IEnumerator TeleportRoutine(GameObject player, Transform dest, GameObject targetMap)
    {
        if (isTeleporting) yield break;
        isTeleporting = true;

        // 🌟 [포탈 암전 시작]
        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
        {
            Debug.Log("<color=magenta>[디버그] 포탈 이동 암전 시도</color>");
            yield return StartCoroutine(FadeCanvasGroup(UIManager.Instance.fadePanel, 1f, 0.5f));
        }

        Rigidbody2D rb = player.GetComponent<Rigidbody2D>();
        if (rb != null) rb.linearVelocity = Vector2.zero;

        if (targetMap != null)
        {
            targetMap.SetActive(true);
            ApplyMapSettings(targetMap);
        }

        player.transform.position = dest.position;
        var nt = player.GetComponent<FishNet.Component.Transforming.NetworkTransform>();
        if (nt != null) nt.Teleport();

        SetCameraTarget(player.transform);
        yield return new WaitForSeconds(0.3f);

        // 🌟 [포탈 암전 해제]
        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
        {
            Debug.Log("<color=magenta>[디버그] 포탈 이동 암전 해제 시도</color>");
            yield return StartCoroutine(FadeCanvasGroup(UIManager.Instance.fadePanel, 0f, 0.5f));
        }

        isTeleporting = false;
    }

    public void ForceFindReferences()
    {
        if (virtualCamera == null)
        {
            CinemachineCamera[] cams = FindObjectsByType<CinemachineCamera>(FindObjectsInactive.Exclude, FindObjectsSortMode.None);
            foreach (var c in cams)
            {
                if (c.gameObject.scene.name == "MainScene")
                {
                    virtualCamera = c;
                    confiner = virtualCamera.GetComponent<CinemachineConfiner2D>();
                    break;
                }
            }
        }
    }

    public void ApplyMapSettings(GameObject targetMap)
    {
        if (targetMap == null) return;
        ForceFindReferences();
        MapInfo info = targetMap.GetComponent<MapInfo>();
        if (info != null && virtualCamera != null)
        {
            LensSettings currentLens = virtualCamera.Lens;
            currentLens.OrthographicSize = info.cameraSize;
            virtualCamera.Lens = currentLens;
            if (info.mapBounds != null) SetCameraBounds(info.mapBounds);
            PlayMusic(info.bgmIndex);
        }
    }

    public void SetCameraTarget(Transform playerTransform)
    {
        ForceFindReferences();
        if (virtualCamera != null)
        {
            virtualCamera.Follow = playerTransform;
            WarpCamera(playerTransform);
        }
    }

    public void WarpCamera(Transform target)
    {
        if (virtualCamera == null) return;
        virtualCamera.ForceCameraPosition(new Vector3(target.position.x, target.position.y, -10f), Quaternion.identity);
        virtualCamera.PreviousStateIsValid = false;
    }

    public void SetCameraBounds(Collider2D newCollider)
    {
        if (confiner != null && newCollider != null)
        {
            confiner.BoundingShape2D = newCollider;
            confiner.InvalidateBoundingShapeCache();
        }
    }

    public void PlayMusic(int index)
    {
        if (bgmSource == null || bgmClips == null || index >= bgmClips.Length) return;
        if (bgmSource.clip == bgmClips[index] && bgmSource.isPlaying) return;
        bgmSource.Stop();
        bgmSource.clip = bgmClips[index];
        bgmSource.Play();
    }
}