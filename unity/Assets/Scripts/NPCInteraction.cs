using UnityEngine;
using UnityEngine.UI;
using TMPro;
using System.Collections;
using System.Collections.Generic;

public class NPCInteraction : MonoBehaviour
{
    [Header("대화 설정")]
    public string npcName = "마빈";
    [TextArea(10, 20)] 
    public List<string> dialogueSentences; 
    public float typingSpeed = 0.05f;

    [Header("UI 연결")]
    public GameObject dialoguePanel; 
    public TextMeshProUGUI uiText;   

    [Header("일반 대화 중 버튼들 (2개)")]
    public GameObject nextButtonA; 
    public GameObject nextButtonB; 

    [Header("마지막 대화 중 버튼들 (2개)")]
    public GameObject lastButtonA; 
    public GameObject lastButtonB; 

    [Header("데이터 연동 테스트 (나중에 DB값으로 대체)")]
    public int mockTicketCount = 8; // DB에서 받아올 교환권 개수 테스트용

    private bool isPlayerNearby = false;
    private bool isTyping = false; 
    private int currentLineIndex = 0; 
    private Coroutine typingCoroutine;
    private string currentFullSentence; 

    void Awake()
    {
        ForceHideAllButtons();
        if (dialoguePanel) dialoguePanel.SetActive(false);
    }

    void Update()
    {
        if (GlobalChatManager.IsChatFocused) return;
        if (isPlayerNearby && Input.GetKeyDown(KeyCode.Space))
        {
            OnInteraction();
        }
    }

    public void OnInteraction()
    {
        if (!dialoguePanel.activeSelf) StartConversation();
        else if (isTyping) FinishTyping();
        else DisplayNextSentence();
    }

    void StartConversation()
    {
        dialoguePanel.SetActive(true);
        currentLineIndex = 0;
        DisplayNextSentence();
    }

    void DisplayNextSentence()
    {
        ForceHideAllButtons();

        if (currentLineIndex < dialogueSentences.Count)
        {
            if (typingCoroutine != null) StopCoroutine(typingCoroutine);
            
            // --- 데이터 치환 로직 시작 ---
            // 1. 원본 문장을 가져옵니다.
            string rawSentence = dialogueSentences[currentLineIndex];

            // 2. 문장 내 {count}라는 글자가 있으면 mockTicketCount 숫자로 바꿉니다.
            // 필요하다면 .Replace("{name}", playerName) 처럼 계속 이어서 쓸 수 있습니다.
            string processedSentence = rawSentence.Replace("{count}", mockTicketCount.ToString());

            // 3. 최종 문장을 구성합니다.
            currentFullSentence = $"[{npcName}]\n{processedSentence}";
            // --- 데이터 치환 로직 끝 ---

            typingCoroutine = StartCoroutine(TypeSentence(currentFullSentence));
            currentLineIndex++; 
        }
        else
        {
            EndConversation();
        }
    }

    IEnumerator TypeSentence(string sentence)
    {
        isTyping = true;
        uiText.text = ""; 

        ForceHideAllButtons();

        foreach (char letter in sentence.ToCharArray())
        {
            uiText.text += letter;
            yield return new WaitForSeconds(typingSpeed);
        }
        
        isTyping = false;
        ShowCorrectButtons();
    }

    void ForceHideAllButtons()
    {
        if (nextButtonA) nextButtonA.SetActive(false);
        if (nextButtonB) nextButtonB.SetActive(false);
        if (lastButtonA) lastButtonA.SetActive(false);
        if (lastButtonB) lastButtonB.SetActive(false);
    }

    void ShowCorrectButtons()
    {
        bool isLast = (currentLineIndex == dialogueSentences.Count);
        
        if (isLast)
        {
            if (lastButtonA) lastButtonA.SetActive(true);
            if (lastButtonB) lastButtonB.SetActive(true);
        }
        else
        {
            if (nextButtonA) nextButtonA.SetActive(true);
            if (nextButtonB) nextButtonB.SetActive(true);
        }
    }

    void FinishTyping()
    {
        if (typingCoroutine != null) StopCoroutine(typingCoroutine);
        uiText.text = currentFullSentence; 
        isTyping = false;
        ShowCorrectButtons(); 
    }

    public void EndConversation()
    {
        dialoguePanel.SetActive(false);
        if (typingCoroutine != null) StopCoroutine(typingCoroutine);
        isTyping = false;
        ForceHideAllButtons();
    }

    private void OnTriggerEnter2D(Collider2D other)
    {
        if (other.CompareTag("Player")) isPlayerNearby = true;
    }

    private void OnTriggerExit2D(Collider2D other)
    {
        if (other.CompareTag("Player"))
        {
            isPlayerNearby = false;
            EndConversation();
        }
    }
}