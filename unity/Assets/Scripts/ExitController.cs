using UnityEngine;
using System.Collections;

public class ExitController : MonoBehaviour
{
    // [완전 종료] 버튼에 이 함수를 연결하세요.
    public void FinalQuit()
    {
        Debug.Log("<color=red>[Exit] 시스템을 종료합니다.</color>");
        // 중복 클릭 방지 등을 위해 버튼 비활성화 처리를 추가하면 좋습니다.
        StartCoroutine(SafeExitRoutine());
    }

    private IEnumerator SafeExitRoutine()
    {
        // 1. 암전 시작
        if (UIManager.Instance != null && UIManager.Instance.fadePanel != null)
        {
            // 암전 패널 활성화
            UIManager.Instance.fadePanel.SetActive(true);
            CanvasGroup cg = UIManager.Instance.fadePanel.GetComponent<CanvasGroup>();

            if (cg != null)
            {
                float duration = 0.8f; // 0.8초 동안 어두워짐 (원하는 대로 조절)
                float time = 0;
                float startAlpha = cg.alpha;

                while (time < duration)
                {
                    time += Time.deltaTime;
                    cg.alpha = Mathf.Lerp(startAlpha, 1f, time / duration);
                    yield return null;
                }
                cg.alpha = 1f;
            }
        }

        // 2. 네트워크 및 스팀 로비 정리
        if (SteamManager.Instance != null)
        {
            SteamManager.Instance.LeaveLobby();
        }

        // 3. 정리 시간 확보 (암전 후 아주 잠깐의 여유)
        yield return new WaitForSecondsRealtime(0.2f);

        // 4. 실제 종료
        Debug.Log("<color=gray>[Exit] 프로세스 종료</color>");
#if UNITY_EDITOR
        UnityEditor.EditorApplication.isPlaying = false;
#else
        Application.Quit();
#endif
    }
}