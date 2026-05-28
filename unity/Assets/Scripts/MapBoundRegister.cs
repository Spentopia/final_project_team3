using UnityEngine;
using System.Collections;

public class MapBoundRegister : MonoBehaviour
{
    [SerializeField] private Collider2D mapCollider;

    void Start()
    {
        if (mapCollider == null) mapCollider = GetComponent<Collider2D>();
        StartCoroutine(RegisterRoutine());
    }

    IEnumerator RegisterRoutine()
    {
        yield return new WaitForSeconds(0.5f); 
        if (GlobalManager.Instance != null && mapCollider != null)
        {
            // GlobalManager에 정의된 함수명과 일치시킴
            GlobalManager.Instance.SetCameraBounds(mapCollider);
        }
    }
}