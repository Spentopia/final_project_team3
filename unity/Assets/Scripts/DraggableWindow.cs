using UnityEngine;
using UnityEngine.EventSystems; // Essential for drag events
using UnityEngine.UI;

public class DraggableWindow : MonoBehaviour, IBeginDragHandler, IDragHandler
{
    [Header("References")]
    // 이 변수에 움직일 전체 인벤토리 창(RectTransform)을 연결합니다.
    public RectTransform windowToMove; 
    
    // 클릭한 지점과 창의 원래 위치 사이의 거리
    private Vector2 pointerOffset; 

    // 드래그를 시작할 때 호출됩니다. (클릭 시 오프셋 계산)
    public void OnBeginDrag(PointerEventData eventData) 
    {
        // 캔버스 내에서의 정확한 위치 계산을 위해 RectTransformUtility를 사용합니다.
        RectTransformUtility.ScreenPointToLocalPointInRectangle(
            windowToMove.parent as RectTransform, 
            eventData.position, 
            eventData.pressEventCamera, 
            out pointerOffset
        );

        // 현재 창의 위치에서 클릭 지점까지의 오프셋을 구합니다.
        pointerOffset -= windowToMove.anchoredPosition;
    }

    // 드래그하는 중에 계속 호출됩니다. (창 위치 업데이트)
    public void OnDrag(PointerEventData eventData) 
    {
        Vector2 localPointerPosition;
        // 마우스의 로컬 캔버스 위치를 계산합니다.
        RectTransformUtility.ScreenPointToLocalPointInRectangle(
            windowToMove.parent as RectTransform, 
            eventData.position, 
            eventData.pressEventCamera, 
            out localPointerPosition
        );
        
        // 계산된 위치에서 오프셋을 빼서 창의 새 위치를 적용합니다.
        // 이렇게 하면 마우스 클릭 지점이 창을 자연스럽게 따라갑니다.
        windowToMove.anchoredPosition = localPointerPosition - pointerOffset;
    }
}