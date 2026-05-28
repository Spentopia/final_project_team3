using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class FadeManager : MonoBehaviour
{
    public static FadeManager Instance;
    public CanvasGroup fadeGroup;
    public bool isManualFade = false; // GlobalManager에서 참조하는 변수

    void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            // 부모가 없을 때만 파괴 방지 (GlobalManager와 충돌 방지)
            if (transform.parent == null) DontDestroyOnLoad(gameObject);
        }
        else if (Instance != this)
        {
            Destroy(gameObject);
        }
    }

    // --- GlobalManager.cs 에러 해결을 위한 반환형 수정 (void -> IEnumerator) ---
    // 이제 StartCoroutine(FadeManager.Instance.FadeIn(1.0f)) 호출이 가능해집니다.
    public IEnumerator FadeIn(float duration)
    {
        if (fadeGroup == null) yield break;

        float timer = 0f;
        while (timer < duration)
        {
            timer += Time.unscaledDeltaTime;
            fadeGroup.alpha = Mathf.Lerp(1f, 0f, timer / duration);
            yield return null;
        }
        fadeGroup.alpha = 0f;
        fadeGroup.blocksRaycasts = false;
    }

    public IEnumerator FadeOut(float duration)
    {
        if (fadeGroup == null) yield break;
        fadeGroup.blocksRaycasts = true;

        float timer = 0f;
        while (timer < duration)
        {
            timer += Time.unscaledDeltaTime;
            fadeGroup.alpha = Mathf.Lerp(0f, 1f, timer / duration);
            yield return null;
        }
        fadeGroup.alpha = 1f;
    }

    // SteamManager 처럼 코루틴 시작 없이 바로 쓰고 싶을 때를 위한 함수 (선택 사항)
    public void StartFadeOut(float duration)
    {
        StopAllCoroutines();
        StartCoroutine(FadeOut(duration));
    }
}