using UnityEngine;
using FishNet.Object; // 🌟 권한(NetworkObject) 확인을 위해 FishNet 추가!

public class LocalPortal : MonoBehaviour
{
    [Header("도착 지점 (같은 맵 내부)")]
    public Transform destination;

    private bool isPlayerInRange = false;
    private GameObject playerObject;

    private void Update()
    {
        if (isPlayerInRange && playerObject != null)
        {
            if (Input.GetKeyDown(KeyCode.UpArrow) || Input.GetKeyDown(KeyCode.W))
            {
                InstantTeleport(playerObject);
            }
        }
    }

    private void OnTriggerEnter2D(Collider2D collision)
    {
        if (collision.CompareTag("Player"))
        {
            // 🌟 [핵심 1] 포탈에 닿은 캐릭터가 "내가 조종하는 내 캐릭터"일 때만 작동!
            var no = collision.GetComponent<NetworkObject>();
            if (no != null && no.IsOwner)
            {
                isPlayerInRange = true;
                playerObject = collision.gameObject;
            }
        }
    }

    private void OnTriggerExit2D(Collider2D collision)
    {
        if (collision.CompareTag("Player"))
        {
            var no = collision.GetComponent<NetworkObject>();
            if (no != null && no.IsOwner)
            {
                isPlayerInRange = false;
                playerObject = null;
            }
        }
    }

    private void InstantTeleport(GameObject player)
    {
        if (destination == null) return;

        // 🌟 [추가] 텔레포트 직후 미끄러지는 현상 방지
        Rigidbody2D rb = player.GetComponent<Rigidbody2D>();
        if (rb != null) rb.linearVelocity = Vector2.zero;

        // 1. 캐릭터 물리적 위치 이동
        player.transform.position = destination.position;

        // 🌟 [핵심 2] FishNet에게 순간이동 사실을 공식적으로 보고!!!
        var nt = player.GetComponent<FishNet.Component.Transforming.NetworkTransform>();
        if (nt != null)
        {
            nt.Teleport(); // 👈 이 한 줄이 여태까지 모든 '유령 현상'의 원인이었습니다!
        }

        // 2. 시네머신에게 알림 (기존 코드 유지)
        if (GlobalManager.Instance != null && GlobalManager.Instance.virtualCamera != null)
        {
            GlobalManager.Instance.virtualCamera.OnTargetObjectWarped(
                player.transform, 
                Vector3.zero 
            );
        }

        isPlayerInRange = false; 
    }
}