using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;

public class CoordinationToggle : MonoBehaviour
{
    public static CoordinationToggle Instance; // 🌟 인벤토리 매니저에서 접근하기 위한 싱글톤

    public Button myButton;
    public GameObject coordinationWindow;
    public UICharacterManager characterManager;

    [Header("코디창 부위별 아이콘 이미지 설정")]
    // 🌟 유니티 인스펙터에서 각 _Icon 오브젝트의 Image 컴포넌트를 직접 드래그 앤 드롭으로 연결하세요!
    public Image hairIcon;
    public Image hatIcon;
    public Image weaponIcon;
    public Image clothesIcon;
    public Image pantsIcon;
    public Image shoesIcon;

    [Header("기본/빈 슬롯 아이콘 (선택사항)")]
    public Sprite defaultEmptySprite; // 아이템을 벗었을 때 보여줄 빈 슬롯 이미지 (없으면 투명 처리)

    private void Awake()
    {
        // 싱글톤 주소는 올바르니 유지합니다.
        if (Instance == null)
        {
            Instance = this;
            Debug.Log("<color=lime><b>[Toggle]</b> 오리지널 코디창 싱글톤 등록 완료.</color>");
        }
        else
        {
            Destroy(this);
            return;
        }
    }

    void Start()
    {
        if (myButton != null)
        {
            myButton.onClick.RemoveAllListeners();
            myButton.onClick.AddListener(OnClickCoordinationButton);
            Debug.Log("<color=white><b>[Toggle]</b> 버튼 리스너 연결됨.</color>");
        }
    }

    void Update()
    {
        // 🌟 [임시 추적 코드] 실시간으로 이 스크립트가 누구와 연결되어 있는지 감시합니다.
        if (Input.GetKeyDown(KeyCode.E))
        {
            Debug.Log($"<color=red><b>[키 입력 감지]</b> 현재 작동 중인 스크립트 오브젝트 이름: {gameObject.name} | 인스턴스 일치여부: {Instance == this} | HairIcon 연결상태: {hairIcon != null}</color>");
        }

        if (GlobalChatManager.IsChatFocused) return;

        if (Input.GetKeyDown(KeyCode.E))
        {
            OnClickCoordinationButton();
        }
    }

    public void OnClickCoordinationButton()
    {
        Debug.Log("<color=yellow><b>[Toggle]</b> 클릭 혹은 단축키 발생!</color>");
        if (coordinationWindow == null) return;

        bool targetState = !coordinationWindow.activeSelf;
        coordinationWindow.SetActive(targetState);

        if (targetState)
        {
            // 캔버스 및 레이아웃 강제 갱신
            Canvas.ForceUpdateCanvases();
            RectTransform rect = coordinationWindow.GetComponent<RectTransform>();
            LayoutRebuilder.ForceRebuildLayoutImmediate(rect);

            rect.anchoredPosition = Vector2.zero;
            rect.localScale = Vector3.one;

            // 🌟 3D 캐릭터 아바타 클론 리프레시 호출
            RefreshCharacterModel();
        }

        Debug.Log($"<color=orange><b>[결과]</b> targetState: {targetState} | 실제 활성여부: {coordinationWindow.activeInHierarchy}</color>");
    }

    /// <summary>
    /// 🌟 FishNet을 기반으로 진짜 '내 캐릭터 본체'를 찾아서 3D 클론을 새로 그리는 함수입니다.
    /// </summary>
    public void RefreshCharacterModel()
    {
        if (characterManager == null) return;

        GameObject realPlayer = null;

        // FishNet에서 제공하는 커넥션을 타고 '진짜 내 로컬 캐릭터'를 타겟팅
        if (FishNet.InstanceFinder.ClientManager.Connection != null)
        {
            var localConnection = FishNet.InstanceFinder.ClientManager.Connection.FirstObject;
            if (localConnection != null)
            {
                realPlayer = localConnection.gameObject;
            }
        }

        // 빽업용 안전장치: 만약 FishNet을 통한 감지가 실패했다면 기존 방식으로 전환
        if (realPlayer == null)
        {
            realPlayer = GameObject.FindWithTag("Player");
            Debug.LogWarning("<color=red><b>[Toggle]</b> 로컬 플레이어를 찾지 못해 FindWithTag를 사용했습니다.</color>");
        }

        // 진짜 내 캐릭터를 바탕으로 UI 3D 더미를 생성 및 복사합니다.
        if (realPlayer != null)
        {
            characterManager.RefreshUIClone(realPlayer);
            Debug.Log("<color=cyan><b>[Toggle]</b> 내 로컬 플레이어를 기반으로 3D 아바타 갱신 완료!</color>");
        }
    }

    /// <summary>
    /// ⭕ [완벽 동기화 버전] 매개변수 타입을 object로 넓혀 Enum 버그를 차단하고 안전하게 주입합니다.
    /// </summary>
    public void UpdateEquipmentSlotUI(object slotName, Sprite itemSprite)
    {
        if (slotName == null)
        {
            Debug.LogWarning("<color=yellow>[Toggle] 전달된 slotName이 null입니다.</color>");
            return;
        }

        // 🌟 [핵심 변경 사항] 넘어온 데이터가 Enum이든 뭐든 간에 순수한 소문자 문자열 글자로 강제 추출합니다.
        string lowerSlot = slotName.ToString().Replace(" ", "").ToLower();
        Image targetIcon = null;

        // 1. 순수 소문자 텍스트 매칭 검사
        if (lowerSlot.Contains("hair"))
            targetIcon = hairIcon;
        else if (lowerSlot.Contains("hat") || lowerSlot.Contains("cap"))
            targetIcon = hatIcon;
        else if (lowerSlot.Contains("clothes") || lowerSlot.Contains("top") || lowerSlot.Contains("body"))
            targetIcon = clothesIcon;
        else if (lowerSlot.Contains("pants") || lowerSlot.Contains("bottom"))
            targetIcon = pantsIcon;
        else if (lowerSlot.Contains("shoes") || lowerSlot.Contains("foot"))
            targetIcon = shoesIcon;
        else if (lowerSlot.Contains("weapon") || lowerSlot.Contains("tool"))
            targetIcon = weaponIcon;

        // 2. 타겟 아이콘 주입 및 uGUI 렌더링 방어막 가동
        if (targetIcon != null)
        {
            if (itemSprite != null)
            {
                targetIcon.sprite = itemSprite;

                // 투명도가 꼬였거나 컴포넌트가 꺼져서 에셋이 안 보이는 현상 차단
                targetIcon.color = Color.white;
                targetIcon.enabled = true;

                Debug.Log($"<color=lime><b>[UI 동기화 성공]</b> {targetIcon.gameObject.name} 슬롯의 Source Image를 [{itemSprite.name}]로 변경했습니다!</color>");
            }
            else
            {
                // 아이템을 벗었을 때 처리
                targetIcon.sprite = defaultEmptySprite;
                if (defaultEmptySprite == null)
                {
                    // 기본 스프라이트가 없으면 투명하게 만듭니다.
                    targetIcon.color = new Color(1, 1, 1, 0);
                }
                else
                {
                    targetIcon.color = Color.white;
                }
                Debug.Log($"[UI 동기화] {lowerSlot} 슬롯을 빈 상태로 초기화했습니다.");
            }
        }
        else
        {
            // 만약 변환된 문자열 매칭이 통과하지 못하면 어떤 글자가 들어왔는지 정확하게 꼬투리를 잡습니다.
            Debug.LogError($"<color=red><b>[UI 동기화 실패]</b> 들어온 원본 데이터: '{slotName}' (최종 해석본: {lowerSlot})에 일치하는 UI 슬롯(Image 변수)을 찾지 못했습니다!</color>");
        }
    }
}