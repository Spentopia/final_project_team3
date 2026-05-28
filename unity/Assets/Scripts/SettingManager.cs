using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class SettingManager : MonoBehaviour
{
    [Header("Panels")]
    public GameObject graphicPanel;
    public GameObject soundPanel;

    [Header("Target UI")]
    public GameObject settingBg;
    
    [Header("Volume UI")]
    public Slider volumeSlider; 
    public Image muteButtonImage;
    public Sprite onSprite;
    public Sprite offSprite;

    [Header("Resolution & Screen Mode")]
    public TMP_Dropdown resolutionDropdown;
    public TMP_Dropdown screenModeDropdown; // 0: 창모드, 1: 전체화면

    private Vector3 initialPosition;
    
    // --- 저장된 데이터 (확정된 값) ---
    private float savedVolume = 1.0f; 
    private bool savedIsMuted = false;
    private int savedResolutionIndex = 0;
    private int savedScreenMode = 0; // 0: Windowed, 1: FullScreen

    // --- 임시 조절 변수 ---
    private bool currentIsMuted = false;
    private int currentResolutionIndex = 0;
    private int currentScreenMode = 0;
    
    private bool isResetting = false;

    void Awake()
    {
        if (settingBg != null)
            initialPosition = settingBg.transform.localPosition;

        // 1. 데이터 로드
        savedVolume = PlayerPrefs.GetFloat("MasterVolume", 1.0f);
        savedIsMuted = PlayerPrefs.GetInt("IsMuted", 0) == 1;
        savedResolutionIndex = PlayerPrefs.GetInt("ResolutionIndex", 0);
        savedScreenMode = PlayerPrefs.GetInt("ScreenMode", 0);

        // 2. 시스템 적용
        ApplySystemSettings(savedVolume, savedIsMuted, savedResolutionIndex, savedScreenMode);
    }

    void Start()
    {
        ResetSettingsState();
    }

    private void ApplySystemSettings(float vol, bool mute, int resIndex, int modeIndex)
    {
        AudioListener.volume = mute ? 0f : vol;

        int width = 1280, height = 720;
        switch (resIndex)
        {
            case 0: width = 1280; height = 720; break;
            case 1: width = 1600; height = 900; break;
            case 2: width = 1920; height = 1080; break;
            case 3: width = 2560; height = 1440; break;
            case 4: width = 3840; height = 2160; break;
        }

        FullScreenMode mode = (modeIndex == 1) ? FullScreenMode.FullScreenWindow : FullScreenMode.Windowed;
        Screen.SetResolution(width, height, mode);
    }

    public void OpenSettings(GameObject settingsUI)
    {
        settingsUI.SetActive(true);
        ResetSettingsState();
    }

    private void ResetSettingsState()
    {
        isResetting = true;

        // 저장된 진짜 데이터로 변수 원복
        savedVolume = PlayerPrefs.GetFloat("MasterVolume", 1.0f);
        savedIsMuted = PlayerPrefs.GetInt("IsMuted", 0) == 1;
        savedResolutionIndex = PlayerPrefs.GetInt("ResolutionIndex", 0);
        savedScreenMode = PlayerPrefs.GetInt("ScreenMode", 0);

        currentIsMuted = savedIsMuted;
        currentResolutionIndex = savedResolutionIndex;
        currentScreenMode = savedScreenMode;

        // 패널 초기화
        if (graphicPanel != null) graphicPanel.SetActive(true);
        if (soundPanel != null) soundPanel.SetActive(false);
        if (settingBg != null) settingBg.transform.localPosition = initialPosition;

        // UI 요소들 원복
        if (volumeSlider != null)
        {
            volumeSlider.onValueChanged.RemoveAllListeners();
            volumeSlider.value = savedVolume * 100f;
            volumeSlider.onValueChanged.AddListener(OnSliderChanged);

            SoundSlider ss = volumeSlider.GetComponent<SoundSlider>();
            if (ss != null)
            {
                volumeSlider.onValueChanged.AddListener(ss.UpdateText);
                ss.UpdateText(volumeSlider.value);
            }
        }

        if (resolutionDropdown != null)
        {
            resolutionDropdown.onValueChanged.RemoveAllListeners();
            resolutionDropdown.value = savedResolutionIndex;
            resolutionDropdown.onValueChanged.AddListener(OnResolutionChanged);
        }

        if (screenModeDropdown != null)
        {
            screenModeDropdown.onValueChanged.RemoveAllListeners();
            screenModeDropdown.value = savedScreenMode;
            screenModeDropdown.onValueChanged.AddListener(OnScreenModeChanged);
        }

        if (muteButtonImage != null)
            muteButtonImage.sprite = currentIsMuted ? offSprite : onSprite;

        isResetting = false;
    }

    // --- 이벤트 핸들러 ---
    public void OnSliderChanged(float value)
    {
        if (isResetting) return;
        if (!currentIsMuted) AudioListener.volume = value / 100f;
    }

    public void OnMuteButtonClicked()
    {
        currentIsMuted = !currentIsMuted;
        if (muteButtonImage != null)
            muteButtonImage.sprite = currentIsMuted ? offSprite : onSprite;
        
        AudioListener.volume = currentIsMuted ? 0f : (volumeSlider.value / 100f);
    }

    public void OnResolutionChanged(int index)
    {
        if (isResetting) return;
        currentResolutionIndex = index;
    }

    public void OnScreenModeChanged(int index)
    {
        if (isResetting) return;
        currentScreenMode = index;
    }

    // --- 확인 / 취소 ---
    public void OnConfirmButtonClicked()
    {
        savedVolume = volumeSlider.value / 100f;
        savedIsMuted = currentIsMuted;
        savedResolutionIndex = currentResolutionIndex;
        savedScreenMode = currentScreenMode;

        PlayerPrefs.SetFloat("MasterVolume", savedVolume);
        PlayerPrefs.SetInt("IsMuted", savedIsMuted ? 1 : 0);
        PlayerPrefs.SetInt("ResolutionIndex", savedResolutionIndex);
        PlayerPrefs.SetInt("ScreenMode", savedScreenMode);
        PlayerPrefs.Save();

        ApplySystemSettings(savedVolume, savedIsMuted, savedResolutionIndex, savedScreenMode);
        settingBg.SetActive(false);
    }

    public void OnCancelButtonClicked()
    {
        ResetSettingsState();
        settingBg.SetActive(false);
    }

    public void ShowGraphicPanel() { graphicPanel.SetActive(true); soundPanel.SetActive(false); }
    public void ShowSoundPanel() { graphicPanel.SetActive(false); soundPanel.SetActive(true); }
}