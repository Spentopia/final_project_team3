using UnityEngine;
using FishNet;
using FishNet.Managing.Scened;
using FishNet.Connection; // 이게 중요합니다!
using FishNet.Transporting; // 🌟 상태 체크를 위해 추가!
using System.Collections;

public class IntroManager : MonoBehaviour
{
    [Header("이동할 메인 씬 이름")]
    public string mainSceneName = "MainScene";

    private void Start()
    {
        Screen.SetResolution(1280, 720, false);
    }

    public void StartAsHost()
    {
        Debug.Log("[IntroManager] 호스트로 게임을 엽니다.");
        if (!InstanceFinder.IsServerStarted)
        {
            InstanceFinder.ServerManager.StartConnection();
            InstanceFinder.ClientManager.StartConnection();
        }
        StartCoroutine(HostFadeAndLoadScene());
    }

    public void StartAsClient(string ip = "localhost")
    {
        Debug.Log($"[IntroManager] 난입 시도 중... 서버 IP: {ip}");

        if (!InstanceFinder.IsClientStarted)
        {
            // 🌟 코루틴을 호출하여 페이드가 끝날 때까지 기다린 후 연결하게 함
            StartCoroutine(ClientConnectRoutine(ip));
        }
    }

    private IEnumerator ClientConnectRoutine(string ip)
    {
        // 1. 먼저 페이드 아웃을 시작하고 완료될 때까지 기다림
        if (FadeManager.Instance != null)
        {
            FadeManager.Instance.isManualFade = true;
            yield return StartCoroutine(FadeManager.Instance.FadeOut(1.0f));
        }

        // 2. 페이드가 끝난 후 연결 시도
        InstanceFinder.ClientManager.StartConnection(ip);

        // 3. 연결 성공 이벤트를 구독
        InstanceFinder.ClientManager.OnClientConnectionState += HandleClientConnectionState;
    }

    // 🌟 이 함수가 클래스 안에 있어야 오류가 안 납니다!
    private void HandleClientConnectionState(ClientConnectionStateArgs args)
    {
        if (args.ConnectionState == LocalConnectionState.Started)
        {
            Debug.Log("[IntroManager] 서버 연결 성공! 페이드 인 실행.");
            
            if (FadeManager.Instance != null)
            {
                StartCoroutine(FadeManager.Instance.FadeIn(1.0f));
                FadeManager.Instance.isManualFade = false;
            }

            // 이벤트 중복 호출 방지를 위해 구독 해제
            InstanceFinder.ClientManager.OnClientConnectionState -= HandleClientConnectionState;
        }
    }

    private IEnumerator HostFadeAndLoadScene()
    {
        if (FadeManager.Instance != null)
        {
            FadeManager.Instance.isManualFade = true;
            yield return StartCoroutine(FadeManager.Instance.FadeOut(1.0f));
        }

        SceneLoadData sld = new SceneLoadData(mainSceneName);
        sld.ReplaceScenes = ReplaceOption.All;
        
        if (InstanceFinder.SceneManager != null)
        {
            InstanceFinder.SceneManager.LoadGlobalScenes(sld);
        }
    }
}