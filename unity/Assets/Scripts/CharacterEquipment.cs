using UnityEngine;
using FishNet.Object;
using FishNet.Object.Synchronizing;

public class CharacterEquipment : NetworkBehaviour
{
    [Header("슬롯 설정")]
    public SpriteRenderer hatSlot;
    public SpriteRenderer hairSlot;
    public SpriteRenderer centerClothes, leftClothes, rightClothes;
    public SpriteRenderer centerPants, leftPants, rightPants;
    public SpriteRenderer leftShoes, rightShoes;

    // 🌐 네트워크 상태 게시판 (신발 추가)
    public readonly SyncVar<string> netHatID = new SyncVar<string>("");
    public readonly SyncVar<string> netClothesID = new SyncVar<string>("");
    public readonly SyncVar<string> netPantsID = new SyncVar<string>("");
    public readonly SyncVar<string> netHairID = new SyncVar<string>("");
    public readonly SyncVar<string> netShoesID = new SyncVar<string>("");

    public override void OnStartNetwork()
    {
        base.OnStartNetwork();
        netHatID.OnChange += (o, n, s) => OnPartChanged(n, "Hat");
        netClothesID.OnChange += (o, n, s) => OnPartChanged(n, "Clothes");
        netPantsID.OnChange += (o, n, s) => OnPartChanged(n, "Pants");
        netHairID.OnChange += (o, n, s) => OnPartChanged(n, "Hair");
        netShoesID.OnChange += (o, n, s) => OnPartChanged(n, "Shoes");
    }

    // 장착 로직
    public void EquipItem(ItemData d)
    {
        if (d == null) return;
        ApplyEquipmentVisuals(d);
        if (IsOwner) ServerEquipItem(d.itemID, d.partType);

        // 🌟 추가: 내 캐릭터라면 UI 인형도 갱신
        if (IsOwner)
        {
            // UICharacterManager를 찾아서 호출 (미리 변수로 선언해두는 것이 좋습니다)
            var uiMgr = FindFirstObjectByType<UICharacterManager>();
            if (uiMgr != null)
            {
                uiMgr.RefreshUIClone(this.gameObject);
            }
        }
    }

    // 모두 벗기기
    [ContextMenu("모두 벗기기")]
    public void ClearAll()
    {
        ApplyClearVisuals();
        if (IsOwner) ServerClearAll();

        // 🌟 추가: 내 캐릭터라면 UI 인형도 갱신
        if (IsOwner)
        {
            var uiMgr = FindFirstObjectByType<UICharacterManager>();
            if (uiMgr != null)
            {
                uiMgr.RefreshUIClone(this.gameObject);
            }
        }
    }

    [ServerRpc]
    private void ServerEquipItem(string id, string type)
    {
        if (type == "Hat") netHatID.Value = id;
        else if (type == "Clothes") netClothesID.Value = id;
        else if (type == "Pants") netPantsID.Value = id;
        else if (type == "Hair") netHairID.Value = id;
        else if (type == "Shoes") netShoesID.Value = id;
    }

    [ServerRpc]
    private void ServerClearAll()
    {
        netHatID.Value = netClothesID.Value = netPantsID.Value = netHairID.Value = netShoesID.Value = "";
    }

    // 수정된 부분: type 인자를 추가하고, ID가 비었을 때 시각적 초기화를 수행합니다.
    private void OnPartChanged(string id, string type)
    {
        // 나 자신이 아닐 때(타인)뿐만 아니라, 나 자신도 서버 응답을 받고 갱신해야 합니다.
        RefreshPart(id, type);
    }

    private void RefreshPart(string id, string type)
    {
        // 🌟 [핵심 수정] ID가 비어있다면 해당 부위의 스프라이트를 지웁니다.
        if (string.IsNullOrEmpty(id))
        {
            ClearPartVisual(type);
            return;
        }

        ItemData item = ItemDatabase.Instance?.GetItem(id);
        if (item != null) ApplyEquipmentVisuals(item);
    }

    // 시각화 로직
    // 기존 ApplyClearVisuals를 이렇게 바꾸면 중복 코드가 사라지고 관리가 편해집니다.
    private void ApplyClearVisuals()
    {
        ClearPartVisual("Hat");
        ClearPartVisual("Hair");
        ClearPartVisual("Clothes");
        ClearPartVisual("Pants");
        ClearPartVisual("Shoes");
    }

    // 🌟 [추가] 특정 부위만 시각적으로 초기화하는 함수
    private void ClearPartVisual(string type)
    {
        if (type == "Hat") { if (hatSlot) hatSlot.sprite = null; }
        else if (type == "Hair") { if (hairSlot) hairSlot.sprite = null; }
        else if (type == "Clothes")
        {
            if (centerClothes) centerClothes.sprite = null;
            if (leftClothes) leftClothes.sprite = null;
            if (rightClothes) rightClothes.sprite = null;
        }
        else if (type == "Pants")
        {
            if (centerPants) centerPants.sprite = null;
            if (leftPants) leftPants.sprite = null;
            if (rightPants) rightPants.sprite = null;
        }
        else if (type == "Shoes")
        {
            if (leftShoes) leftShoes.sprite = null;
            if (rightShoes) rightShoes.sprite = null;
        }
    }

    private void ApplyEquipmentVisuals(ItemData d)
    {
        void Set(SpriteRenderer sr, Sprite s, PartOffset off, int order = 1)
        {
            if (!sr) return;
            sr.sprite = s;
            sr.sortingOrder = order;
            sr.transform.localPosition = off.positionOffset;
            sr.transform.localScale = off.scaleOffset;
            sr.transform.localRotation = Quaternion.Euler(0, 0, off.rotationZ);
        }

        if (d.partType == "Hat") Set(hatSlot, d.centerSprite, d.centerOffset);
        else if (d.partType == "Clothes") {
            Set(centerClothes, d.centerSprite, d.centerOffset);
            Set(leftClothes, d.leftSprite, d.leftOffset);
            Set(rightClothes, d.rightSprite, d.rightOffset);
        }
        else if (d.partType == "Pants") {
            Set(centerPants, d.centerSprite, d.centerOffset);
            Set(leftPants, d.leftSprite, d.leftOffset);
            Set(rightPants, d.rightSprite, d.rightOffset);
        }
        else if (d.partType == "Shoes") {
            Set(leftShoes, d.leftShoeSprite, d.leftShoeOffset, 2);
            Set(rightShoes, d.rightShoeSprite, d.rightShoeOffset, 2);
        }
        else if (d.partType == "Hair") Set(hairSlot, d.centerSprite, d.centerOffset);
    }
}