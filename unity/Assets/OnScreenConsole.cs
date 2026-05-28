using UnityEngine;
using System.Collections.Generic;
using FishNet;
using FishNet.Transporting;

public class OnScreenConsole : MonoBehaviour
{
    private List<string> logs = new List<string>();
    private Vector2 scrollPosition;

    void Awake()
    {
        // 씬이 넘어가도 에러 창이 계속 살아있도록 설정
        DontDestroyOnLoad(gameObject);
        
        // 유니티의 모든 디버그 로그와 에러를 가로채서 화면에 저장
        Application.logMessageReceived += HandleLog;
    }

    void Start()
    {
        // FishNet 네트워크 끊김 이벤트 감지
        if (InstanceFinder.ClientManager != null)
        {
            InstanceFinder.ClientManager.OnClientConnectionState += OnClientState;
        }
    }

    private void OnClientState(ClientConnectionStateArgs args)
    {
        Debug.Log($"[네트워크 상태] 클라이언트 상태 변경됨: {args.ConnectionState}");
        if (args.ConnectionState == LocalConnectionState.Stopped)
        {
            Debug.LogError($"🚨 [네트워크 끊김] 서버와의 연결이 종료되었습니다! 씬 이동 실패 또는 프리팹 누락을 의심해보세요.");
        }
    }

    void HandleLog(string logString, string stackTrace, LogType type)
    {
        string logEntry = $"[{type}] {logString}";
        
        // 에러나 예외 발생 시 스택 트레이스(에러가 난 정확한 줄 번호)도 함께 출력
        if (type == LogType.Error || type == LogType.Exception)
        {
            logEntry += $"\n{stackTrace}";
        }

        logs.Add(logEntry);
        
        // 화면에 로그가 너무 꽉 차면 예전 것부터 삭제
        if (logs.Count > 40) logs.RemoveAt(0);
        
        // 새로운 에러가 뜰 때마다 스크롤을 맨 아래로 자동 이동
        scrollPosition.y = Mathf.Infinity; 
    }

    void OnGUI()
    {
        // 화면 전체를 덮는 투명한 박스 생성
        GUILayout.BeginArea(new Rect(20, 20, Screen.width - 40, Screen.height - 40));
        
        // 배경색을 살짝 어둡게 (글씨가 잘 보이도록)
        GUI.backgroundColor = new Color(0, 0, 0, 0.8f);
        scrollPosition = GUILayout.BeginScrollView(scrollPosition, "box");

        // 수집된 로그들을 화면에 출력
        foreach (string log in logs)
        {
            // 에러는 빨간색, 경고는 노란색, 나머지는 흰색
            if (log.Contains("[Error]") || log.Contains("[Exception]"))
                GUI.contentColor = Color.red;
            else if (log.Contains("[Warning]"))
                GUI.contentColor = Color.yellow;
            else
                GUI.contentColor = Color.white;
            
            // 글씨 크기를 키워서 보기 편하게 설정
            GUIStyle style = new GUIStyle(GUI.skin.label);
            style.fontSize = 24;
            style.wordWrap = true;

            GUILayout.Label(log, style);
        }

        GUILayout.EndScrollView();
        GUILayout.EndArea();
    }
}