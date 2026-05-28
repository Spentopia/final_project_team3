using System.Collections;
using System.Collections.Generic;
using Unity.VisualScripting;
using UnityEngine;
using UnityEngine.Networking;
using UnityEngine.UI;

// =========================================================
// 1. 서버 통신용 데이터 구조
// =========================================================
[System.Serializable]
public class ServerItemData
{
    public string inventory_id;
    public string item_id;
    public string name;
    public bool is_equipped;
    public string slot_name;
    public string wallet_address;
    public string token_id;
    public string contract_address;
}

// =========================================================
// 2. 인벤토리 매니저 메인 클래스
// =========================================================
public class InventoryManager : MonoBehaviour
{
    public static InventoryManager Instance;

    [Header("UI Reference Settings")]
    public GameObject mainCanvas;
    public GameObject inventoryPanel;

    [Header("Inventory Settings")]
    public Transform slotParent;
    public GameObject slotPrefab;
    public List<InventorySlot> slots = new List<InventorySlot>();
    private bool isOpened = false;

    // 🌟 초기 위치 저장을 위한 변수
    private Vector3 initialPosition;

    [Header("API Settings")]
    private string inventoryApiUrl = "https://api.spentopia.net/api/unity/avatar/inventory";

    [Header("연결된 내 캐릭터 장비 시스템")]
    public CharacterEquipment localPlayerEquipment;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;

            if (mainCanvas != null) mainCanvas.SetActive(false);
            if (inventoryPanel != null)
            {
                // 게임 시작 시 인벤토리의 초기 위치를 기억합니다.
                initialPosition = inventoryPanel.transform.localPosition;
                inventoryPanel.SetActive(false);
            }
        }
        else
        {
            Destroy(gameObject);
        }
    }

    private void Update()
    {
        if (GlobalChatManager.IsChatFocused) return;

        if (Input.GetKeyDown(KeyCode.I))
        {
            ToggleInventory();
        }
    }

    void Start()
    {
        InitializeInventory();
    }

    private void InitializeInventory()
    {
        slots.Clear();
        for (int i = 0; i < 100; i++) CreateNewSlot();
    }

    private void CreateNewSlot()
    {
        if (slotPrefab == null) return;
        GameObject newSlotObj = Instantiate(slotPrefab, slotParent);
        InventorySlot newSlot = newSlotObj.GetComponent<InventorySlot>();
        if (newSlot != null) slots.Add(newSlot);
    }

    // =========================================================
    // 3. 서버 통신 로직
    // =========================================================
    public void LoadInventoryFromServer(string accessToken)
    {
        StartCoroutine(FetchInventoryRoutine(accessToken));
    }

    private IEnumerator FetchInventoryRoutine(string accessToken)
    {
        using (UnityWebRequest webRequest = UnityWebRequest.Get(inventoryApiUrl))
        {
            webRequest.SetRequestHeader("Authorization", "Bearer " + accessToken);
            yield return webRequest.SendWebRequest();

            if (webRequest.result == UnityWebRequest.Result.Success)
            {
                string jsonResponse = webRequest.downloadHandler.text;
                Debug.Log("인벤토리 데이터 수신 완료: " + jsonResponse);
                ApplyDataToSlots(jsonResponse);
            }
            else
            {
                Debug.LogError("인벤토리 불러오기 실패 (상태 코드): " + webRequest.error);
                Debug.LogError("🚨 서버 상세 에러 내용: " + webRequest.downloadHandler.text);
            }
        }
    }

    private void ApplyDataToSlots(string json)
    {
        ServerItemData[] items = JsonHelper.FromJson<ServerItemData>(json);
        if (items == null) return;

        // 🌟 중복된 item_id를 체크하기 위한 바구니(HashSet) 생성
        HashSet<string> processedItemIds = new HashSet<string>();

        int slotIndex = 0; // 슬롯 칸을 채울 인덱스 변수

        for (int i = 0; i < items.Length; i++)
        {
            // 만약 이번 아이템의 item_id가 이미 처리된(바구니에 있는) 거라면?
            if (processedItemIds.Contains(items[i].item_id))
            {
                // 🚫 더 이상 슬롯에 담지 않고 다음 아이템으로 그냥 넘어갑니다(패스).
                continue;
            }

            // 처음 보는 item_id라면 바구니에 이름을 적어 둡니다.
            processedItemIds.Add(items[i].item_id);

            // 안전하게 인벤토리 슬롯 한계(100칸)를 넘지 않도록 체크하며 배치
            if (slotIndex < slots.Count)
            {
                slots[slotIndex].SetItem(items[i]);
                slotIndex++; // 🌟 아이템을 실제로 배치했을 때만 다음 슬롯 칸으로 이동!
            }
        }
    }

    // =========================================================
    // 4. 캐릭터 장착 명령 로직 (🌟 코디창 슬롯 UI 및 아바타 동기화 적용)
    // =========================================================
    public void EquipItem(ItemData data)
    {
        if (data == null)
        {
            Debug.LogError("<color=red><b>[EquipItem 에러]</b> 전달된 ItemData가 null입니다!</color>");
            return;
        }

        Debug.Log($"<color=white><b>[EquipItem 시작]</b> 아이템 이름: {data.itemName} | 부위: {data.partType} | 아이콘 존재여부: {data.inventoryIcon != null}</color>");

        if (localPlayerEquipment != null)
        {
            // 1. 실제 월드의 3D 캐릭터에게 아이템 장착 수행
            localPlayerEquipment.EquipItem(data);
            Debug.Log("<color=lime><b>[EquipItem]</b> 1. localPlayerEquipment.EquipItem(data) 실행 완료.</color>");

            // 2. 🌟 [코디창 UI 동기화 검증]
            if (CoordinationToggle.Instance != null)
            {
                Debug.Log($"<color=cyan><b>[EquipItem]</b> 2. CoordinationToggle 인스턴스 발견! UI 슬롯 갱신 함수를 호출합니다. (전달 값: {data.partType}, {data.inventoryIcon?.name})</color>");

                CoordinationToggle.Instance.UpdateEquipmentSlotUI(data.partType, data.inventoryIcon);

                // 3. 만약 코디창 패널이 화면에 열려 있는 상태라면, 3D 아바타 모양도 즉시 새로고침해 줍니다.
                if (CoordinationToggle.Instance.coordinationWindow != null)
                {
                    bool isWindowActive = CoordinationToggle.Instance.coordinationWindow.activeSelf;
                    Debug.Log($"<color=magenta><b>[EquipItem]</b> 3. 코디창 윈도우 오브젝트 상태 체크 ➡️ 현재 활성화(On) 상태인가?: {isWindowActive}</color>");

                    if (isWindowActive)
                    {
                        CoordinationToggle.Instance.RefreshCharacterModel();
                        Debug.Log("<color=orange><b>[EquipItem]</b> 코디창이 열려있어 3D 아바타 클론 리프레시를 수행했습니다.</color>");
                    }
                }
                else
                {
                    Debug.LogWarning("<color=yellow><b>[EquipItem 경고]</b> CoordinationToggle 안의 coordinationWindow 오브젝트가 인스펙터에서 연결되지 않았습니다!</color>");
                }
            }
            else
            {
                Debug.LogError("<color=red><b>[EquipItem 에러]</b> CoordinationToggle.Instance가 null입니다! CoordinationToggle 스크립트가 붙은 오브젝트가 하이어라키에서 비활성화(Deactive) 되어있는지 확인하세요.</color>");
            }
        }
        else
        {
            Debug.LogWarning("<color=yellow><b>[EquipItem 경고]</b> 내 캐릭터(CharacterEquipment)가 아직 연결되지 않았습니다!</color>");
        }
    }

    // =========================================================
    // UI 제어 함수들 (🌟 위치 복구 로직 포함)
    // =========================================================
    public void SetupMainSceneUI()
    {
        if (mainCanvas != null) mainCanvas.SetActive(true);
        ToggleInventory(false);
        Cursor.visible = true;
        Cursor.lockState = CursorLockMode.None;
    }

    public void ToggleInventory(bool forceState)
    {
        if (inventoryPanel == null) return;

        isOpened = forceState;
        inventoryPanel.SetActive(isOpened);

        // 인벤토리가 켜질 때 위치를 원래대로 돌려놓습니다.
        if (isOpened)
        {
            inventoryPanel.transform.localPosition = initialPosition;
        }
    }

    public void ToggleInventory()
    {
        ToggleInventory(!isOpened);
    }

    public void OnClearAllButtonClicked()
    {
        if (localPlayerEquipment != null)
        {
            // 1. 실제 3D 캐릭터의 장비를 모두 해제합니다.
            localPlayerEquipment.ClearAll();
            Debug.Log("[InventoryManager] 🗑️ 모두 벗기기 버튼이 눌렸습니다!");

            // 2. 🌟 [코디창 UI 동기화] 장비를 다 벗었으므로 코디창의 우측 2D 슬롯 이미지들도 전부 빈칸으로 초기화합니다.
            if (CoordinationToggle.Instance != null)
            {
                CoordinationToggle.Instance.UpdateEquipmentSlotUI("hair", null);
                CoordinationToggle.Instance.UpdateEquipmentSlotUI("hat", null);
                CoordinationToggle.Instance.UpdateEquipmentSlotUI("weapon", null);
                CoordinationToggle.Instance.UpdateEquipmentSlotUI("clothes", null);
                CoordinationToggle.Instance.UpdateEquipmentSlotUI("pants", null);
                CoordinationToggle.Instance.UpdateEquipmentSlotUI("shoes", null);

                // 3. 코디창이 켜져 있다면 다 벗은 깔끔한 상태의 3D 아바타로 실시간 리프레시 합니다.
                if (CoordinationToggle.Instance.coordinationWindow != null &&
                    CoordinationToggle.Instance.coordinationWindow.activeSelf)
                {
                    CoordinationToggle.Instance.RefreshCharacterModel();
                }
            }
        }
        else
        {
            Debug.LogWarning("[InventoryManager] 옷을 벗길 캐릭터가 아직 연결되지 않았습니다.");
        }
    }
}

// =========================================================
// 5. 대괄호 배열 [ ] 파싱을 위한 안전한 JsonHelper
// =========================================================
public static class JsonHelper
{
    public static T[] FromJson<T>(string json)
    {
        // 1. 만약 문자열이 비어있다면 빈 배열을 리턴해서 오류 방지
        if (string.IsNullOrEmpty(json)) return new T[0];

        // 2. 유니티 JsonUtility가 읽을 수 있게 강제로 껍데기를 씌움
        string newJson = "{\"items\":" + json + "}";

        try
        {
            Wrapper<T> wrapper = JsonUtility.FromJson<Wrapper<T>>(newJson);

            // 3. 🚨 [핵심 해결책] 파싱된 데이터 중에 구조체가 완전히 깨졌거나 
            // 알맹이가 비어있는(null) 쓰레기 데이터들을 리스트에서 완전히 걸러냅니다.
            if (wrapper != null && wrapper.items != null)
            {
                List<T> validList = new List<T>();
                for (int i = 0; i < wrapper.items.Length; i++)
                {
                    // 데이터가 실제로 존재하는 녀석들만 압축해서 담아줍니다.
                    if (wrapper.items[i] != null)
                    {
                        validList.Add(wrapper.items[i]);
                    }
                }
                return validList.ToArray(); // [A]만 담긴 순수한 크기 1짜리 배열 리턴!
            }
        }
        catch (System.Exception e)
        {
            Debug.LogError("[JsonHelper 에러] JSON 파싱 중 오류 발생: " + e.Message);
        }

        return new T[0];
    }

    [System.Serializable]
    private class Wrapper<T>
    {
        public T[] items;
    }
}