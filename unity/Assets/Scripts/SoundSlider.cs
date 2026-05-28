using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class SoundSlider : MonoBehaviour
{
    // SettingManager가 직접 찾아서 쓸 수 있도록 변수 연결만 해두면 됩니다.
    public TextMeshProUGUI valueText;

    // Start에서 AddListener 하던 부분을 삭제했습니다. 
    // (SettingManager에서 관리하는 것이 더 확실하기 때문)
    
    public void UpdateText(float value)
    {
        if (valueText != null)
        {
            valueText.text = Mathf.RoundToInt(value).ToString();
        }
    }
}