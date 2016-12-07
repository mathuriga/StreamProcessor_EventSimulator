package org.wso2.carbon.event.simulator.core.randomdatafeedsimulation.bean;

/**
 * Created by mathuriga on 23/11/16.
 */


public class PropertyBasedAttributeDto extends StreamAttributeDto {
    private String category;
    private String property;

    public PropertyBasedAttributeDto(String type, String category, String property) {
        super();
        this.category = category;
        this.property = property;
    }

    public PropertyBasedAttributeDto(){
        super();
    }
    public String getCategory() {
        return category;
    }

//    @XmlElement
    public void setCategory(String category) {
        this.category = category;
    }

    public String getProperty() {
        return property;
    }

//    @XmlElement
    public void setProperty(String property) {
        this.property = property;
    }
}