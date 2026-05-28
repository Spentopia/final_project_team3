using UnityEngine;
using System.Collections.Generic;

public class MapManager : MonoBehaviour
{
    public static MapManager Instance;
    public List<GameObject> allMaps;

    private void Awake() => Instance = this;

    public void SwitchMap(GameObject targetMap, Transform playerTransform)
    {
        if (targetMap == null) return;

        // 1. 먼저 맵들을 전부 끕니다.
        foreach (var map in allMaps) { if (map != null) map.SetActive(false); }

        // 2. 타겟 맵을 켭니다.
        targetMap.SetActive(true);

        // 3. [개선] 맵 자식들 중 "MapBounds" 태그를 가진 녀석만 정밀 타격!
        Collider2D mapCollider = null;

        // 모든 자식을 뒤져서 찾음 (비활성화 상태여도 찾음)
        foreach (Transform child in targetMap.GetComponentsInChildren<Transform>(true))
        {
            if (child.CompareTag("MapBounds"))
            {
                mapCollider = child.GetComponent<Collider2D>();
                break;
            }
        }

        if (mapCollider != null)
        {
            Debug.Log($"[MapManager] {targetMap.name}의 경계선을 찾았습니다: {mapCollider.name}");
            if (GlobalManager.Instance != null)
            {
                GlobalManager.Instance.SetCameraBounds(mapCollider);
            }
        }
        else
        {
            Debug.LogError($"[MapManager] {targetMap.name}에서 'MapBounds' 태그를 가진 콜라이더를 찾을 수 없습니다!");
        }
    }
}