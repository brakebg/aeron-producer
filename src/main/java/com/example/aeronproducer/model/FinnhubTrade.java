package com.example.aeronproducer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinnhubTrade {
    @JsonProperty("s")
    private String symbol;
    
    @JsonProperty("p")
    private Double price;
    
    @JsonProperty("t")
    private Long timestamp;
    
    @JsonProperty("v")
    private Double volume;
    
    @JsonProperty("c")
    private String[] conditions;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public Double getVolume() { return volume; }
    public void setVolume(Double volume) { this.volume = volume; }
    
    public String[] getConditions() { return conditions; }
    public void setConditions(String[] conditions) { this.conditions = conditions; }
    
    @Override
    public String toString() {
        return String.format("FinnhubTrade{symbol='%s', price=%.2f, volume=%.0f, timestamp=%d}", 
                symbol, price, volume, timestamp);
    }
}