using UnityEngine;
using UnityEngine.UI;

public class SimpleTestSlot : MonoBehaviour
{
    public ItemData myItem; // 인스펙터에서 테스트할 모자나 옷을 넣는 곳

    void Start()
    {
        // 이 버튼을 누르면 인벤토리 매니저의 장착 함수를 바로 호출!
        GetComponent<Button>().onClick.AddListener(() => {
            if (myItem != null)
            {
                InventoryManager.Instance.EquipItem(myItem);
                Debug.Log($"<color=white>{myItem.itemName} 장착 버튼 클릭!</color>");
            }
        });
    }
}