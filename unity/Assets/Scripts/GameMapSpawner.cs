using UnityEngine;
using FishNet;
using FishNet.Object;
using FishNet.Connection;

public class GameMapSpawner : NetworkBehaviour
{
    [Header("생성할 플레이어 프리팹 (body_man)")]
    public GameObject playerPrefab;

    [Header("스폰 위치 (SpawnPoint_Start)")]
    public Transform spawnPoint;

    public override void OnStartClient()
    {
        base.OnStartClient();
        Debug.Log("[GameMapSpawner] 맵 로드 완료! 서버에 캐릭터 생성을 요청합니다.");
        
        // 클라이언트가 씬에 접속하면 무조건 서버에 스폰을 요청합니다.
        RequestSpawnPlayerServerRpc();
    }

    [ServerRpc(RequireOwnership = false)]
    private void RequestSpawnPlayerServerRpc(NetworkConnection caller = null)
    {
        if (playerPrefab == null) return;
        
        // 이 유저가 이미 캐릭터를 가지고 있다면 중복 생성 방지
        if (caller.FirstObject != null) return;

        Vector3 pos = spawnPoint != null ? spawnPoint.position : Vector3.zero;
        GameObject player = Instantiate(playerPrefab, pos, Quaternion.identity);

        // 🌟 스폰 직후, 해당 유저의 고유 번호(ClientId)를 캐릭터에게 넘겨줍니다.
        // % 100을 해서 0~99 사이의 번호가 Sorting Order로 들어가게 합니다.
        var nameTag = player.GetComponent<PlayerNameTag>();
        if (nameTag != null)
        {
            nameTag.SetLayerIndex(caller.ClientId % 100);
        }

        UnityEngine.SceneManagement.SceneManager.MoveGameObjectToScene(player, gameObject.scene);
        InstanceFinder.ServerManager.Spawn(player, caller);
    }
}