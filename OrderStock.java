public class OrderStock {
private String trader_name;
private int ID;
private String type;
private int quantity;  // how many stocks you wanna buy
private String company; //company name (ex:AAPl)
private double price; // Whta is the price of the stock

// Constructor Method
public OrderStock(String trader_name, int ID, String type,String company,int quantity, double price){
    this.ID = ID;
    this.type = type;
    this.company = company;
    this.quantity = quantity;
    this.price = price;
}

public String getTrader(){
    return trader_name;
}



public int getID(){
    return ID;
}

public String getCompany(){
    return company;
}

public int getQuantity(){
    return quantity;
} 

public double getPrice(){
    return price;
}
public String getType(){
    return type;
}

public int setQuantity(int newQuantity){
    this.quantity = newQuantity;
    return quantity;
}



public boolean eqauls(OrderStock other) {
    return this.trader_name.equals(trader_name) &&
           this.ID == other.ID &&
           this.type.equalsIgnoreCase(other.type) &&
           this.company.equalsIgnoreCase(other.company) &&
           this.quantity == other.quantity &&
           this.price == other.price;
}






// print stocks Example-: BUY 10 AAPL @ 185.50
public String toString(){
    return String.format("%-4d %-6s %-10s %-6d %-10.2f",
    ID, type, company, quantity, price);

}
}



