// The steps a customer goes through during one visit, in order.
// Customer moves itself between these states, and the rest of the game reads
// the state to decide what is allowed next (e.g. only a SEATED customer can
// have an order taken). An enum is used so that no invalid state can exist.
public enum CustomerState{
    WAITING,      
    SEATED,       
    ORDERED,     
    SERVED,      
    READY_TO_PAY, 
    PAID,       
    LEFT          
}
