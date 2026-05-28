using UnityEngine;
using UnityEngine.UI;

public class ImageToggle : MonoBehaviour
{
    public Sprite onSprite;  // 소리 켜짐 이미지
    public Sprite offSprite; // 음소거 이미지
    
    private Image targetImage;
    private bool isOn = true;

    void Awake()
    {
        targetImage = GetComponent<Image>();
    }

    public void ToggleImage()
    {
        isOn = !isOn; // 상태 반전
        
        // 상태에 따라 이미지 교체
        targetImage.sprite = isOn ? onSprite : offSprite;

        // 실제 마스터 볼륨 음소거 (0: 음소거, 1: 켜짐)
        AudioListener.volume = isOn ? 1f : 0f;
    }
}