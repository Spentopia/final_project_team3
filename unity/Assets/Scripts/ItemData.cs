using UnityEngine;

// 🌟 [수정] 파츠별로 위치, 크기, 그리고 '회전'까지 관리하기 위한 구조체
[System.Serializable]
public struct PartOffset
{
    public Vector2 positionOffset;
    public Vector2 scaleOffset;
    public float rotationZ; // 🌟 [신규] Z축 회전값 (도(Degree) 단위)
}

[CreateAssetMenu(fileName = "New Item", menuName = "Items/ItemData")]
public class ItemData : ScriptableObject 
{
    [Header("서버 연동 정보")]
    public string itemID; 

    [Header("기본 정보")]
    public string itemName;
    public string partType; // Hat, Clothes, Pants, Hair
    public Sprite inventoryIcon;
    
    [Header("캐릭터 장착용 파츠 이미지")]
    public Sprite centerSprite;
    public Sprite leftSprite;  
    public Sprite rightSprite; 

    [Header("신발 이미지")]
    public Sprite leftShoeSprite;
    public Sprite rightShoeSprite;

    [Header("파츠별 위치, 크기, 회전 조정")]
    // 회전값(rotationZ)은 float의 기본값인 0으로 자동 초기화됩니다.
    public PartOffset centerOffset = new PartOffset { scaleOffset = new Vector2(1f, 1f) };
    public PartOffset leftOffset = new PartOffset { scaleOffset = new Vector2(1f, 1f) };
    public PartOffset rightOffset = new PartOffset { scaleOffset = new Vector2(1f, 1f) };

    [Header("신발 오프셋")]
    public PartOffset leftShoeOffset = new PartOffset { scaleOffset = new Vector2(1f, 1f) };
    public PartOffset rightShoeOffset = new PartOffset { scaleOffset = new Vector2(1f, 1f) };
}