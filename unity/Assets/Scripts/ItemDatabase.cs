using System.Collections.Generic;
using UnityEngine;

public class ItemDatabase : MonoBehaviour
{
    public static ItemDatabase Instance; // 🌟 이거 추가
    // 인스펙터 창에서 아이템 데이터 파일들을 드래그해서 넣어둘 리스트
    public List<ItemData> allItems; 
    
    // 서버 ID로 아이템을 빠르게 찾기 위한 딕셔너리
    private Dictionary<string, ItemData> itemDict = new Dictionary<string, ItemData>();

    void Awake()
    {
        Instance = this; // 🌟 이거 추가
        // 딕셔너리에 데이터 등록
        foreach (var item in allItems)
        {
            if (!itemDict.ContainsKey(item.itemID))
                itemDict.Add(item.itemID, item);
        }
    }

    public ItemData GetItem(string id)
    {
        if (itemDict.TryGetValue(id, out ItemData item))
            return item;
        
        Debug.LogWarning($"ID {id}에 해당하는 아이템을 찾을 수 없습니다.");
        return null;
    }
}