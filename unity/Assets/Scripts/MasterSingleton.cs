using UnityEngine;

public class MasterSingleton : MonoBehaviour
{
    public static MasterSingleton Instance;

    void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject); // 부모가 살아남으면 자식들도 다 같이 삼!
        }
        else
        {
            // 이미 존재한다면, 새로 태어난 덩어리 전체를 파괴
            Destroy(gameObject);
        }
    }
}