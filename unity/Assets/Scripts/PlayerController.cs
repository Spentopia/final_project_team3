using UnityEngine;
using FishNet.Object;

public class PlayerController : NetworkBehaviour
{
    private Rigidbody2D rb;

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
    }

public override void OnStartClient()
{
    base.OnStartClient();
    
    if (IsOwner)
    {
        if (GlobalManager.Instance != null) 
            GlobalManager.Instance.SetCameraTarget(this.transform);
    }
    else
    {
        if (rb != null)
        {
            // 🌟 1. Kinematic으로 변경
            rb.bodyType = RigidbodyType2D.Kinematic;
            
            // 🌟 2. 물리 연산(중력, 충돌 등)을 완전히 끕니다. (가장 중요!)
            rb.simulated = false; 
            
            // 🌟 3. 혹시나 충돌체(Collider)가 물리 엔진과 상호작용하는 것을 방지
            Collider2D col = GetComponent<Collider2D>();
            if (col != null) col.enabled = false;
        }
        }
    }
}