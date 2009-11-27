/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chinacnd.b2b.paper.service.product;

import com.chinacnd.b2b.paper.dao.product.CategoryDAO;
import com.chinacnd.b2b.paper.dao.product.ExtendAttributeDAO;
import com.chinacnd.b2b.paper.dao.product.ExtendAttributeValueDAO;
import com.chinacnd.b2b.paper.dao.product.ProductDAO;
import com.chinacnd.b2b.paper.entities.product.Category;
import com.chinacnd.b2b.paper.entities.product.CategoryType;
import com.chinacnd.b2b.paper.entities.product.ExtendAttribute;
import com.chinacnd.b2b.paper.entities.product.ExtendAttributeValue;
import com.chinacnd.b2b.paper.entities.product.Paper;
import com.chinacnd.b2b.paper.entities.product.Product;
import com.chinacnd.b2b.paper.entities.product.Pulp;
import com.chinacnd.b2b.paper.exception.ServiceException;
import com.chinacnd.b2b.paper.helper.form.admin.product.ExtendAttributeValueForm;
import com.chinacnd.b2b.paper.helper.form.admin.product.PaperForm;
import com.chinacnd.b2b.paper.helper.form.admin.product.ProductForm;
import com.chinacnd.b2b.paper.helper.form.admin.product.PulpForm;
import com.chinacnd.framework.db.OrderBy;
import com.chinacnd.framework.db.Page;
import com.chinacnd.framework.util.BeanUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Hadeslee
 */
public class ProductService {

    @Resource
    private ProductDAO productDAO;
    @Resource
    private CategoryDAO categoryDAO;
    @Resource
    private ExtendAttributeValueDAO extendAttributeValueDAO;
    @Resource
    private ExtendAttributeDAO extendAttributeDAO;

    @Transactional(readOnly = true)
    public String getCategoryTreePath(ProductForm form) throws ServiceException {
        Product product = findById(form.getId());
        Category category = product.getCategory();
        //如果分类为空，则表示没有分类，此时就要判断Product真正是哪个对象了
        if (category == null) {
            if (product instanceof Paper) {
                category = categoryDAO.findRootByType(CategoryType.PAPER);
            } else if (product instanceof Pulp) {
                category = categoryDAO.findRootByType(CategoryType.PULP);
            } else {
                throw new ServiceException("非法的商品实体数据:" + product);
            }
        }
        return category.getFullCode();
    }

    @Transactional(readOnly = true)
    public List<Product> findProductsWithoutCategory(ProductForm form) {
        Page page = Page.from(form);
        OrderBy orderBy = OrderBy.from(form);
        List<Product> list = productDAO.findProductsWithoutCategory(page, orderBy);
        form.setTotalSize(page.getTotalCount());
        return list;
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productDAO.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ExtendAttributeValue> getExtendAttributeValuesById(Long id) throws ServiceException {
        Product product = findById(id);
        Category category = product.getCategory();
        if (category == null) {
            throw new ServiceException("当前商品不属于任何商品分类，所以不能有扩展属性");
        }
        List<ExtendAttribute> extendAttributes = categoryDAO.getAllExtendAttributes(category.getId());
        List<ExtendAttributeValue> extendAttributeValues = product.getExtendValueList();
        List<ExtendAttributeValue> list = new ArrayList<ExtendAttributeValue>();
        //取交集，不取并集
        for (ExtendAttribute extendAttribute : extendAttributes) {
            boolean find = false;
            for (ExtendAttributeValue value : extendAttributeValues) {
                if (BeanUtils.equals(value.getAttribute(), extendAttribute)) {
                    list.add(value);
                    find = true;
                    break;
                }
            }
            //如果没有找到相关的值，则添加一条空的进去
            if (find == false) {
                ExtendAttributeValue value = new ExtendAttributeValue();
                value.setAttribute(extendAttribute);
                value.setProduct(product);
                list.add(value);
            }
        }
        return list;

    }

    @Transactional(readOnly = true)
    public List<Product> findByCategoryId(ProductForm form) {
        Long categoryId = form.getCategoryId();
        Page page = Page.from(form);
        OrderBy orderBy = OrderBy.from(form);
        List<Product> list = productDAO.findByCategoryId(categoryId, page, orderBy);
        form.setTotalSize(page.getTotalCount());
        return list;
    }

    /**
     * 通过商品的通用属性进行查询
     * @param form
     * @return
     */
    @Transactional(readOnly = true)
    public List<Product> findByProductSearch(ProductForm form) {
        //TODO 实现
        return null;
    }

    /**
     * 通过纸张的属性进行高级查询
     * @param form
     * @return
     */
    @Transactional(readOnly = true)
    public List<Paper> findByPaperSearch(PaperForm form) {
        //TODO 实现
        return null;
    }

    /**
     * 通过纸浆的属性进行高级查询
     * @param form
     * @return
     */
    @Transactional(readOnly = true)
    public List<Pulp> findByPulpSearch(PulpForm form) {
        //TODO 实现
        return null;
    }

    @Transactional
    public void saveCoreAttributeOfPaper(PaperForm form) {
        Paper paper = (Paper) findById(form.getId());
        paper.setLastUpdateDate(new Date());
        BeanUtils.copyProperties(form, paper, "id");
    }

    @Transactional
    public void saveCoreAttributeOfPulp(PulpForm form) {
        Pulp pulp = (Pulp) findById(form.getId());
        pulp.setLastUpdateDate(new Date());
        BeanUtils.copyProperties(form, pulp, "id");
    }

    @Transactional
    public void saveExtendAttribute(List<ExtendAttributeValueForm> values) {
        for (ExtendAttributeValueForm form : values) {
            ExtendAttributeValue value = null;
            if (form.getId() == null || form.getId() <= 0) {
                value = new ExtendAttributeValue();
            } else {
                value = extendAttributeValueDAO.findById(form.getId());
            }
            value.setAttribute(extendAttributeDAO.findById(form.getExtendAttributeId()));
            value.setAttributeValue(form.getAttributeValue());
            value.setProduct(productDAO.findById(form.getProductId()));
            extendAttributeValueDAO.saveOrUpdate(value);
        }
    }

    @Transactional
    public void changeProductCategory(ProductForm form) throws ServiceException {
        Product product = findById(form.getId());
        Category category = categoryDAO.findById(form.getCategoryId());
        if (category == null) {
            throw new ServiceException("非法的商品分类");
        }
        if ((product instanceof Paper && category.getType() == CategoryType.PAPER) ||
                (product instanceof Pulp && category.getType() == CategoryType.PULP)) {
            product.setCategory(category);
        } else {
            throw new ServiceException("不能跨大类进行商品分类");
        }
    }
}