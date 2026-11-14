// Where an order is in its life. Order moves itself between these values;
// Waiter, Chef and Cashier check them before acting
// (e.g. the Waiter can only serve a READY order).
public enum OrderStatus{
    PLACED,    
    PREPARING, 
    READY,    
    SERVED,   
    PAID,      
    CANCELLED  
}
