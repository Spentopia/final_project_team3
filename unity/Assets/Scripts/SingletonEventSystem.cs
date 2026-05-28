using UnityEngine;
using UnityEngine.EventSystems;

public class SingletonEventSystem : MonoBehaviour
{
    private static SingletonEventSystem instance;

    private void Awake()
    {
        if (instance == null)
        {
            instance = this;
            DontDestroyOnLoad(gameObject); // 이 녀석을 불사신으로 만듭니다.
        }
        else
        {
            // 이미 불사신이 된 녀석이 있다면, 새로 태어난 녀석은 바로 제거!
            Destroy(gameObject);
        }
    }
}