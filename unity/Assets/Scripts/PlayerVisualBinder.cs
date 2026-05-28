using UnityEngine;
using FishNet.Object; // 🌟 [추가] FishNet 네임스페이스

// 캐릭터 프리팹 최상단에 붙여줍니다.
[RequireComponent(typeof(CharacterEquipment))] 
public class PlayerVisualBinder : NetworkBehaviour // 🌟 [수정] NetworkBehaviour 상속!
{
    // 🌟 [수정] Start() 대신 FishNet의 OnStartClient()를 사용합니다.
    // (네트워크 스폰이 완료된 시점에 실행됨을 보장하기 위해)
    public override void OnStartClient()
    {
        base.OnStartClient();

        // 🚨 [핵심 방어벽] 이 캐릭터가 "내" 캐릭터인가요?
        if (!base.IsOwner) 
        {
            // 상대방 캐릭터이거나 소유권이 없으면 인벤토리에 연결하지 않고 조용히 빠져나갑니다.
            return; 
        }

        // --- 여기서부터는 오직 "내 캐릭터"만 실행됩니다 ---
        
        if (InventoryManager.Instance != null)
        {
            CharacterEquipment myEquipment = GetComponent<CharacterEquipment>();

            // 매니저에게 내 진짜 캐릭터를 확실하게 꽂아줍니다!
            InventoryManager.Instance.localPlayerEquipment = myEquipment;
            
            Debug.Log("[PlayerVisualBinder] 🟢 진짜 내 네트워크 캐릭터가 InventoryManager에 연결되었습니다!");
        }
    }
}