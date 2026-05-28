using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class InventorySlot : MonoBehaviour, IPointerClickHandler
{
    public Image icon; 
    
    [Header("Item Data")]
    // 1. 유니티 로컬 데이터 (실제 이미지, 장착 부위 정보 등)
    public ItemData currentItemData;
    
    // 2. 서버에서 받아온 데이터 (DB 아이디, NFT 지갑 주소 등)
    public ServerItemData currentServerData; 

    // 기존 방식: 유니티 로컬 ItemData를 직접 넣을 때 사용
    public void AddItem(ItemData newData)
    {
        currentItemData = newData;
        if (icon != null)
        {
            icon.sprite = newData.inventoryIcon; 
            icon.enabled = true;
            icon.color = new Color(1, 1, 1, 1);
        }
    }

    // [새로 추가된 부분] 매니저에서 서버 데이터를 넘겨줄 때 호출되는 함수
public void SetItem(ServerItemData serverData)
{
    currentServerData = serverData;

    // 무거운 FindAnyObjectByType 삭제하고 Instance로 바로 접근!
    if (ItemDatabase.Instance != null)
    {
        ItemData data = ItemDatabase.Instance.GetItem(serverData.item_id);
        if (data != null)
        {
            AddItem(data); // 실제 슬롯에 이미지 적용
            Debug.Log($"[정석 연동 성공] {data.itemName} 슬롯 배치 완료!");
        }
    }
}

    // [수정된 부분] 슬롯을 완벽하게 초기화 (서버 데이터 + 로컬 데이터 모두 삭제)
    public void ClearSlot()
    {
        currentItemData = null;
        currentServerData = null; 
        
        if (icon != null)
        {
            icon.sprite = null;
            icon.enabled = false;
        }
    }

    // 클릭 시 장착 로직
    public void OnPointerClick(PointerEventData eventData)
    {
        // 1. 로컬 데이터(이미지 등)가 연결되어 있을 때
        if (currentItemData != null)
        {
            // FindAnyObjectByType 대신 싱글톤 Instance를 쓰면 성능 저하 없이 바로 접근 가능합니다!
            if (InventoryManager.Instance != null)
            {
                InventoryManager.Instance.EquipItem(currentItemData);
                Debug.Log($"{currentItemData.itemName} 장착 시도!");
            }
        }
        // 2. 서버 데이터만 들어와 있고 아직 로컬 데이터 연결이 안 되었을 때 (방어 코드)
        else if (currentServerData != null)
        {
            Debug.LogWarning($"아이템 데이터는 받았지만, 아직 유니티 이미지(ItemData)와 연결되지 않았습니다: {currentServerData.name}");
        }
    }
}