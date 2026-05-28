using UnityEngine;

public class MapInfo : MonoBehaviour
{
    [Header("카메라 설정")]
    public float cameraSize = 16.9f;
    public PolygonCollider2D mapBounds;

    [Header("음악 설정")]
    public int bgmIndex = 1; // 💡 이 맵에 들어오면 재생할 BGM 인덱스 (기본값 1: 집)
}