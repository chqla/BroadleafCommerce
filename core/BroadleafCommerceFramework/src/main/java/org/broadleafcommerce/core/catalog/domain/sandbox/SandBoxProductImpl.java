package org.broadleafcommerce.core.catalog.domain.sandbox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductAttribute;
import org.broadleafcommerce.core.catalog.domain.ProductDimension;
import org.broadleafcommerce.core.catalog.domain.ProductWeight;
import org.broadleafcommerce.core.catalog.domain.RelatedProduct;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.common.EmbeddedSandBoxItem;
import org.broadleafcommerce.core.catalog.domain.common.SandBoxItem;
import org.broadleafcommerce.core.media.domain.Media;
import org.broadleafcommerce.core.media.domain.sandbox.SandBoxMediaImpl;
import org.broadleafcommerce.presentation.AdminPresentation;
import org.broadleafcommerce.profile.util.DateUtil;
import org.broadleafcommerce.profile.vendor.service.type.ContainerShapeType;
import org.broadleafcommerce.profile.vendor.service.type.ContainerSizeType;
import org.compass.annotations.Searchable;
import org.compass.annotations.SearchableId;
import org.compass.annotations.SearchableProperty;
import org.compass.annotations.SupportUnmarshall;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Table;

/*
 * TODO emit these java files and compile at runtime based on an annotation
 * present in the normal entity. This will be part of special persistence
 * class handling that will be introduced into MergePersistenceUnitManager.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(appliesTo="BLC_PRODUCT_SNDBX", indexes={
		@Index(name="PRODUCT_SNDBX_VER_INDX", columnNames={"VERSION"}),
		@Index(name="PRODUCT_SNDBX_NAME_INDX", columnNames={"NAME"}),
		@Index(name="PRODUCT_CAT_SNDBX_INDEX", columnNames={"DEFAULT_CATEGORY_ID"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
@Searchable(alias="product", supportUnmarshall=SupportUnmarshall.FALSE)
public class SandBoxProductImpl implements Product, SandBoxItem {

	private static final Log LOG = LogFactory.getLog(SandBoxProductImpl.class);
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The id. */
    @Id
    @GeneratedValue(generator = "ProductId", strategy = GenerationType.TABLE)
    @TableGenerator(name = "ProductId", table = "SEQUENCE_GENERATOR", pkColumnName = "ID_NAME", valueColumnName = "ID_VAL", pkColumnValue = "ProductImpl", allocationSize = 50)
    @Column(name = "PRODUCT_ID")
    @SearchableId
    @AdminPresentation(friendlyName="Product ID", group="Primary Key", hidden=true)
    protected Long id;

    /** The name. */
    @Column(name = "NAME", nullable=false)
    @SearchableProperty(name="productName")
    @AdminPresentation(friendlyName="Product Name", order=1, group="Product Description", prominent=true, columnWidth="25%", groupOrder=1)
    protected String name;

    /** The description. */
    @Column(name = "DESCRIPTION")
    @AdminPresentation(friendlyName="Product Description", order=2, group="Product Description", prominent=false, largeEntry=true, groupOrder=1)
    protected String description;

    /** The long description. */
    @Column(name = "LONG_DESCRIPTION")
    @SearchableProperty(name="productDescription")
    @AdminPresentation(friendlyName="Product Long Description", order=3, group="Product Description", prominent=false, largeEntry=true, groupOrder=1)
    protected String longDescription;

    /** The active start date. */
    @Column(name = "ACTIVE_START_DATE")
    @AdminPresentation(friendlyName="Product Active Start Date", order=8, group="Active Date Range", groupOrder=2)
    protected Date activeStartDate;

    /** The active end date. */
    @Column(name = "ACTIVE_END_DATE")
    @AdminPresentation(friendlyName="Product Active End Date", order=9, group="Active Date Range", groupOrder=2)
    protected Date activeEndDate;

    /** The product model number */
    @Column(name = "MODEL")
    @SearchableProperty(name="productModel")
    @AdminPresentation(friendlyName="Product Model", order=4, group="Product Description", prominent=true, groupOrder=1)
    protected String model;

    /** The manufacture name */
    @Column(name = "MANUFACTURE")
    @SearchableProperty(name="productManufacturer")
    @AdminPresentation(friendlyName="Product Manufacturer", order=5, group="Product Description", prominent=true, groupOrder=1)
    protected String manufacturer;

    /** The product dimensions **/
    @Embedded
    protected ProductDimension dimension = new ProductDimension();

    /** The product weight **/
    @Embedded
    protected ProductWeight weight = new ProductWeight();
    
    @Column(name = "IS_FEATURED_PRODUCT", nullable=false)
    @AdminPresentation(friendlyName="Is Featured Product", order=6, group="Product Description", prominent=false)
    protected boolean isFeaturedProduct = false;

    @Column(name = "IS_MACHINE_SORTABLE")
    @AdminPresentation(friendlyName="Is Product Machine Sortable", order=7, group="Product Description", prominent=false)
    protected boolean isMachineSortable = true;
    
    /** The skus. */
    @Transient
    protected List<Sku> skus = new ArrayList<Sku>();
    
    @Transient
    protected String promoMessage;

	@OneToMany(mappedBy = "product", targetEntity = SandBoxCrossSaleProductImpl.class, cascade = {CascadeType.ALL})
    @Cascade(value={org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN})    
    protected List<RelatedProduct> crossSaleProducts = new ArrayList<RelatedProduct>();

    @OneToMany(mappedBy = "product", targetEntity = SandBoxUpSaleProductImpl.class, cascade = {CascadeType.ALL})
    @Cascade(value={org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN})    
    @OrderBy(value="sequence")
    protected List<RelatedProduct> upSaleProducts  = new ArrayList<RelatedProduct>();

    /** The all skus. */
    @ManyToMany(fetch = FetchType.LAZY, targetEntity = SandBoxSkuImpl.class)
    @JoinTable(name = "BLC_PRDCT_SKU_SNDBX_XREF", joinColumns = @JoinColumn(name = "PRODUCT_ID", referencedColumnName = "PRODUCT_ID"), inverseJoinColumns = @JoinColumn(name = "SKU_ID", referencedColumnName = "SKU_ID"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
    @BatchSize(size = 50)
    protected List<Sku> allSkus = new ArrayList<Sku>();

    /** The product images. */
    @CollectionOfElements
    @JoinTable(name = "BLC_PRDCT_SNDBX_IMAGE", joinColumns = @JoinColumn(name = "PRODUCT_ID"))
    @MapKey(columns = { @Column(name = "NAME", length = 5, nullable = false) })
    @Column(name = "URL")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
    @Deprecated
    protected Map<String, String> productImages = new HashMap<String, String>();

    /** The product media. */
    @ManyToMany(targetEntity = SandBoxMediaImpl.class)
    @JoinTable(name = "BLC_PRDCT_MEDIA_SNDBX_MAP", inverseJoinColumns = @JoinColumn(name = "MEDIA_ID", referencedColumnName = "MEDIA_ID"))
    @MapKey(columns = {@Column(name = "MAP_KEY", nullable = false)})
    @Cascade(value={org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
    @BatchSize(size = 50)
    protected Map<String, Media> productMedia = new HashMap<String , Media>();

    /** The default category. */
    @ManyToOne(targetEntity = SandBoxCategoryImpl.class)
    @JoinColumn(name = "DEFAULT_CATEGORY_ID")
    @AdminPresentation(friendlyName="Product Default Category", order=6, group="Product Description")
    protected Category defaultCategory;

    @ManyToMany(fetch = FetchType.LAZY, targetEntity = SandBoxCategoryImpl.class, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(name = "BLC_CAT_PRDCT_SNDBX_XREF", joinColumns = @JoinColumn(name = "PRODUCT_ID"), inverseJoinColumns = @JoinColumn(name = "CATEGORY_ID", referencedColumnName = "CATEGORY_ID", nullable=true))
    @Cascade(value={org.hibernate.annotations.CascadeType.MERGE, org.hibernate.annotations.CascadeType.PERSIST})    
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
    @BatchSize(size = 50)
    protected List<Category> allParentCategories = new ArrayList<Category>();
    
    @OneToMany(mappedBy = "product", targetEntity = SandBoxProductAttributeImpl.class, cascade = {CascadeType.ALL})
    @Cascade(value={org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN})    
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="blStandardElements")
    @BatchSize(size = 50)
    protected List<ProductAttribute> productAttributes  = new ArrayList<ProductAttribute>();
    
    @Embedded
    protected SandBoxItem sandBoxItem = new EmbeddedSandBoxItem();
    
    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getId()
     */
    public Long getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#setId(java.lang.Long)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getDescription()
     */
    public String getDescription() {
        return description;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#setDescription(java.lang
     * .String)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getLongDescription()
     */
    public String getLongDescription() {
        return longDescription;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#setLongDescription(java.
     * lang.String)
     */
    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getActiveStartDate()
     */
    public Date getActiveStartDate() {
        return activeStartDate;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#setActiveStartDate(java.
     * util.Date)
     */
    public void setActiveStartDate(Date activeStartDate) {
        this.activeStartDate = activeStartDate;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getActiveEndDate()
     */
    public Date getActiveEndDate() {
        return activeEndDate;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#setActiveEndDate(java.util
     * .Date)
     */
    public void setActiveEndDate(Date activeEndDate) {
        this.activeEndDate = activeEndDate;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#isActive()
     */
    public boolean isActive() {
        if (LOG.isDebugEnabled()) {
            if (!DateUtil.isActive(getActiveStartDate(), getActiveEndDate(), false)) {
                LOG.debug("product, " + id + ", inactive due to date");
            }
        }
        return DateUtil.isActive(getActiveStartDate(), getActiveEndDate(), false);
    }
    
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public ProductDimension getDimension() {
        return dimension;
    }

    public void setDimension(ProductDimension dimension) {
    	this.dimension = dimension;
    }

    public BigDecimal getWidth() {
        return dimension==null?null:dimension.getWidth();
    }

    public void setWidth(BigDecimal width) {
        dimension.setWidth(width);
    }

    public BigDecimal getHeight() {
        return dimension==null?null:dimension.getHeight();
    }

    public void setHeight(BigDecimal height) {
        dimension.setHeight(height);
    }

    public BigDecimal getDepth() {
        return dimension==null?null:dimension.getDepth();
    }

    public void setDepth(BigDecimal depth) {
        dimension.setDepth(depth);
    }

    public void setGirth(BigDecimal girth) {
        dimension.setGirth(girth);
    }

    public BigDecimal getGirth() {
        return dimension==null?null:dimension.getGirth();
    }

    public ContainerSizeType getSize() {
        return dimension==null?null:dimension.getSize();
    }

    public void setSize(ContainerSizeType size) {
        dimension.setSize(size);
    }

    public ContainerShapeType getContainer() {
        return dimension==null?null:dimension.getContainer();
    }

    public void setContainer(ContainerShapeType container) {
        dimension.setContainer(container);
    }

    /**
     * Returns the product dimensions as a String (assumes measurements are in inches)
     * @return a String value of the product dimensions
     */
    public String getDimensionString() {
        return dimension==null?null:dimension.getDimensionString();
    }
    
    public boolean isFeaturedProduct() {
        return isFeaturedProduct;
    }

    public void setFeaturedProduct(boolean isFeaturedProduct) {
        this.isFeaturedProduct = isFeaturedProduct;
    }

    public boolean isMachineSortable() {
        return isMachineSortable;
    }

    public void setMachineSortable(boolean isMachineSortable) {
        this.isMachineSortable = isMachineSortable;
    }

    public ProductWeight getWeight() {
        return weight;
    }

    public void setWeight(ProductWeight weight) {
        this.weight = weight;
    }

	/**
	 * @return the promoMessage
	 */
	public String getPromoMessage() {
		return promoMessage;
	}

	/**
	 * @param promoMessage the promoMessage to set
	 */
	public void setPromoMessage(String promoMessage) {
		this.promoMessage = promoMessage;
	}

    /**
     * Gets the all skus.
     * @return the all skus
     */
    public List<Sku> getAllSkus() {
        return allSkus;
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getSkus()
     */
    public List<Sku> getSkus() {
        if (skus.size() == 0) {
            List<Sku> allSkus = getAllSkus();
            for (Sku sku : allSkus) {
                if (sku.isActive()) {
                    skus.add(sku);
                }
            }
        }
        return skus;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#setAllSkus(java.util.List)
     */
    public void setAllSkus(List<Sku> skus) {
        this.allSkus.clear();
        for(Sku sku : skus){
        	this.allSkus.add(sku);
        }
        //this.skus.clear();
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getProductImages()
     */
    @Deprecated
    public Map<String, String> getProductImages() {
        return productImages;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#getProductImage(java.lang
     * .String)
     */
    @Deprecated
    public String getProductImage(String imageKey) {
        return productImages.get(imageKey);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.broadleafcommerce.core.catalog.domain.Product#setProductImages(java.util
     * .Map)
     */
    @Deprecated
    public void setProductImages(Map<String, String> productImages) {
        this.productImages.clear();
//        for(String key : productImages.keySet()){
//        	this.productImages.put(key, productImages.get(key));
//        }
    	for(Map.Entry<String, String> me : productImages.entrySet()) {
    		this.productImages.put(me.getKey(), me.getValue());
    	}
    }

    /*
     * (non-Javadoc)
     * @see org.broadleafcommerce.core.catalog.domain.Product#getDefaultCategory()
     */
    public Category getDefaultCategory() {
        return defaultCategory;
    }

    public Map<String, Media> getProductMedia() {
        return productMedia;
    }

    public void setProductMedia(Map<String, Media> productMedia) {
        this.productMedia.clear();
    	for(Map.Entry<String, Media> me : productMedia.entrySet()) {
    		this.productMedia.put(me.getKey(), me.getValue());
    	}
    }

    /*
     * (non-Javadoc)
     * @seeorg.broadleafcommerce.core.catalog.domain.Product#setDefaultCategory(org.
     * broadleafcommerce.catalog.domain.Category)
     */
    public void setDefaultCategory(Category defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public List<Category> getAllParentCategories() {
        return allParentCategories;
    }

    public void setAllParentCategories(List<Category> allParentCategories) {    	
        this.allParentCategories.clear();
        for(Category category : allParentCategories){
        	this.allParentCategories.add(category);
        }
    }

    public List<RelatedProduct> getCrossSaleProducts() {
        return crossSaleProducts;
    }

    public void setCrossSaleProducts(List<RelatedProduct> crossSaleProducts) {
        this.crossSaleProducts.clear();
        for(RelatedProduct relatedProduct : crossSaleProducts){
        	this.crossSaleProducts.add(relatedProduct);
        }    	
    }

    public List<RelatedProduct> getUpSaleProducts() {
        return upSaleProducts;
    }

    public void setUpSaleProducts(List<RelatedProduct> upSaleProducts) {
        this.upSaleProducts.clear();
        for(RelatedProduct relatedProduct : upSaleProducts){
        	this.upSaleProducts.add(relatedProduct);
        }
        this.upSaleProducts = upSaleProducts;
    }

    public List<ProductAttribute> getProductAttributes() {
		return productAttributes;
	}

	public void setProductAttributes(List<ProductAttribute> productAttributes) {
		this.productAttributes = productAttributes;
	}

	/**
	 * @return
	 * @see org.broadleafcommerce.core.catalog.domain.common.SandBoxItem#getVersion()
	 */
	public long getVersion() {
		return sandBoxItem.getVersion();
	}

	/**
	 * @param version
	 * @see org.broadleafcommerce.core.catalog.domain.common.SandBoxItem#setVersion(long)
	 */
	public void setVersion(long version) {
		sandBoxItem.setVersion(version);
	}

	/**
	 * @return
	 * @see org.broadleafcommerce.core.catalog.domain.common.SandBoxItem#isDirty()
	 */
	public boolean isDirty() {
		return sandBoxItem.isDirty();
	}

	/**
	 * @param dirty
	 * @see org.broadleafcommerce.core.catalog.domain.common.SandBoxItem#setDirty(boolean)
	 */
	public void setDirty(boolean dirty) {
		sandBoxItem.setDirty(dirty);
	}

	/**
	 * @return
	 * @see org.broadleafcommerce.core.catalog.domain.common.SandBoxItem#getCommaDelimitedDirtyFields()
	 */
	public String getCommaDelimitedDirtyFields() {
		return sandBoxItem.getCommaDelimitedDirtyFields();
	}

	/**
	 * @param commaDelimitedDirtyFields
	 * @see org.broadleafcommerce.core.catalog.domain.common.SandBoxItem#setCommaDelimitedDirtyFields(java.lang.String)
	 */
	public void setCommaDelimitedDirtyFields(String commaDelimitedDirtyFields) {
		sandBoxItem.setCommaDelimitedDirtyFields(commaDelimitedDirtyFields);
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((skus == null) ? 0 : skus.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SandBoxProductImpl other = (SandBoxProductImpl) obj;

        if (id != null && other.id != null) {
            return id.equals(other.id);
        }

        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (skus == null) {
            if (other.skus != null)
                return false;
        } else if (!skus.equals(other.skus))
            return false;
        return true;
    }
    
}
