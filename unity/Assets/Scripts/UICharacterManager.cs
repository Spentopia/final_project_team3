using UnityEngine;
using System.Collections.Generic;
using System.IO;

#if UNITY_EDITOR
using UnityEditor;
#endif

public class UICharacterManager : MonoBehaviour
{
    [Header("설정")]
    public Transform spawnPoint;
    public string uiLayerName = "UICharacter";
    public Camera characterCamera; // 👈 인스펙터에서 캐릭터 전용 카메라를 연결하세요.

    private GameObject dummyCharacter;

    /// <summary>
    /// 캐릭터 복제 및 UI 배치 (기존 로직 유지)
    /// </summary>
    public void RefreshUIClone(GameObject myRealCharacter)
    {
        if (myRealCharacter == null) return;

        // 1. 기존 더미 제거
        if (dummyCharacter != null) DestroyImmediate(dummyCharacter);

        // 2. 복제 및 기본 설정
        dummyCharacter = Instantiate(myRealCharacter);
        dummyCharacter.SetActive(false);
        dummyCharacter.name = "UI_Dummy_Character";

        // 캐릭터가 보이도록 부모 설정 유지
        dummyCharacter.transform.SetParent(this.transform);

        // 3. 레이어 및 태그 변경
        SetLayerRecursively(dummyCharacter, LayerMask.NameToLayer(uiLayerName));
        dummyCharacter.tag = "Untagged";

        // 4. 기능 정지 및 이름표 제거
        CleanUpEverything(dummyCharacter);

        // [추가] 글로벌 오디오 강제 복구
        AudioListener listener = GetComponentInParent<AudioListener>();
        if (listener != null) listener.enabled = true;

        // 5. 본체 활성화
        dummyCharacter.SetActive(true);

        // 6. 위치 및 애니메이션 최종 박제
        FreezePose(dummyCharacter);
    }

    /// <summary>
    /// 캐릭터 캡처 및 파일 저장 창 띄우기
    /// </summary>
    /// <summary>
    /// 캐릭터 캡처 및 파일 저장 창 띄우기 (화면 파래짐 방지 버전)
    /// </summary>
    public void CaptureCharacter()
    {
        if (dummyCharacter == null) return;

        if (characterCamera == null)
            characterCamera = GameObject.Find("UICharacterCamera")?.GetComponent<Camera>();

        if (characterCamera == null)
        {
            Debug.LogError("UICharacterCamera를 찾을 수 없습니다!");
            return;
        }

        // 1. 설정 저장 (원래 카메라 상태를 기억함)
        RenderTexture previousRT = characterCamera.targetTexture;

        // 2. 렌더 텍스처 생성 및 연결
        int resWidth = 1024;
        int resHeight = 1024;
        RenderTexture rt = new RenderTexture(resWidth, resHeight, 24);
        characterCamera.targetTexture = rt; // 여기서 카메라의 목적지가 파일용으로 바뀝니다.

        Texture2D screenShot = new Texture2D(resWidth, resHeight, TextureFormat.RGBA32, false);

        // 3. 렌더링 실행
        characterCamera.Render();

        RenderTexture.active = rt;
        screenShot.ReadPixels(new Rect(0, 0, resWidth, resHeight), 0, 0);
        screenShot.Apply();

        // 4. [중요] 카메라 복구 (이걸 안 하면 화면이 파랗거나 멈춘 것처럼 보입니다)
        characterCamera.targetTexture = previousRT; // 카메라의 목적지를 다시 원래대로(모니터로) 돌려놓음
        RenderTexture.active = null;

        // 메모리에서 즉시 제거
        rt.Release();
        DestroyImmediate(rt);

        byte[] bytes = screenShot.EncodeToPNG();

        // 5. 파일 선택 창 실행
        string path = "";
#if UNITY_EDITOR
        // 유니티 에디터에서는 편리하게 저장 창을 띄웁니다.
        path = UnityEditor.EditorUtility.SaveFilePanel("캐릭터 이미지 저장", "", "MyCharacter.png", "png");
#else
        // ✅ [빌드 버전용] 바탕화면 경로를 가져와서 자동으로 파일명을 만듭니다.
        string desktopPath = System.Environment.GetFolderPath(System.Environment.SpecialFolder.Desktop);
        string fileName = "MyCharacter_" + System.DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".png";
        path = Path.Combine(desktopPath, fileName);
#endif

        // 6. 실제 파일 저장 실행
        if (!string.IsNullOrEmpty(path))
        {
            File.WriteAllBytes(path, bytes);
            Debug.Log($"📸 저장 완료: {path}");

            // ✅ [빌드 버전 전용 추가] 저장이 완료되면 유저가 바로 확인할 수 있게 바탕화면 폴더를 열어줍니다.
#if !UNITY_EDITOR
            Application.OpenURL(desktopPath); 
#endif
        }

        // Texture2D 메모리 해제 및 마무리
        DestroyImmediate(screenShot);
    }

    private void CleanUpEverything(GameObject obj)
    {
        var allChildren = obj.GetComponentsInChildren<Transform>(true);
        for (int i = allChildren.Length - 1; i >= 0; i--)
        {
            Transform t = allChildren[i];
            if (t == null || t == obj.transform) continue;

            string n = t.name.ToLower();
            if (t.GetComponent<Canvas>() != null || n.Contains("name") || n.Contains("tag") || n.Contains("ui"))
            {
                DestroyImmediate(t.gameObject);
            }
        }

        var behaviors = obj.GetComponentsInChildren<MonoBehaviour>(true);
        foreach (var b in behaviors)
        {
            if (b == null || b is UICharacterManager) continue;

            if (b is AudioListener || b is AudioSource)
            {
                b.enabled = false;
                continue;
            }

            string fullName = b.GetType().FullName;
            if (fullName.Contains("FishNet") && !fullName.Contains("NetworkObject"))
                DestroyImmediate(b);
            else if (fullName.Contains("Controller") || fullName.Contains("Input") || fullName.Contains("Move") || fullName.Contains("Look"))
                b.enabled = false;
        }

        var components = obj.GetComponentsInChildren<Component>(true);
        foreach (var c in components)
        {
            if (c == null || c is AudioListener || c is AudioSource) continue;
            if (c.GetType().FullName.Contains("NetworkObject")) DestroyImmediate(c);
        }

        Rigidbody[] rbs = obj.GetComponentsInChildren<Rigidbody>(true);
        foreach (var rb in rbs)
        {
            if (rb == null) continue;
            rb.isKinematic = true;
            rb.constraints = RigidbodyConstraints.FreezeAll;
        }

        if (obj.TryGetComponent<CharacterController>(out var cc)) cc.enabled = false;
        foreach (var r in obj.GetComponentsInChildren<Renderer>(true)) if (r != null) r.enabled = true;
    }

    private void FreezePose(GameObject obj)
    {
        if (obj.TryGetComponent<Animator>(out var anim))
        {
            anim.enabled = true;
            anim.speed = 0f;
            anim.Play(0, -1, 0f);
            anim.Update(0);
        }

        if (spawnPoint != null)
        {
            obj.transform.position = spawnPoint.position;
            obj.transform.rotation = spawnPoint.rotation;
        }
    }

    private void SetLayerRecursively(GameObject obj, int newLayer)
    {
        if (obj == null) return;
        obj.layer = newLayer;
        foreach (Transform child in obj.transform) SetLayerRecursively(child.gameObject, newLayer);
    }

    private void Update() { LockTransform(); }
    private void LateUpdate() { LockTransform(); }

    private void LockTransform()
    {
        if (dummyCharacter != null && spawnPoint != null)
        {
            dummyCharacter.transform.position = spawnPoint.position;
            dummyCharacter.transform.rotation = spawnPoint.rotation;
        }
    }
}