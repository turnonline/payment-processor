/*
 * Copyright (c) 2020 TurnOnline.biz s.r.o. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package biz.turnonline.ecosystem.payment.api;

import biz.turnonline.ecosystem.payment.api.model.Category;
import biz.turnonline.ecosystem.payment.service.CategoryService;
import biz.turnonline.ecosystem.payment.service.PaymentConfig;
import biz.turnonline.ecosystem.payment.service.model.CommonTransaction;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiReference;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.InternalServerErrorException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.common.base.MoreObjects;
import ma.glasnost.orika.MapperFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.categoryNotFoundMessage;
import static biz.turnonline.ecosystem.payment.api.EndpointsCommon.tryAgainLaterMessage;

/**
 * REST API Endpoint of the payment processor's categories.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
@Api
@ApiReference( EndpointsApiProfile.class )
public class CategoryEndpoint
{
    private static final Logger logger = LoggerFactory.getLogger( CategoryEndpoint.class );

    private final EndpointsCommon common;

    private final MapperFacade mapper;

    private final CategoryService service;

    private final PaymentConfig config;

    @Inject
    CategoryEndpoint( EndpointsCommon common,
                      MapperFacade mapper,
                      CategoryService service,
                      PaymentConfig config )
    {
        this.common = common;
        this.mapper = mapper;
        this.service = service;
        this.config = config;
    }

    @ApiMethod( name = "category.list", path = "categories", httpMethod = ApiMethod.HttpMethod.GET )
    public List<Category> listCategories( User authUser )
            throws Exception
    {
        common.authorize( authUser );
        List<Category> categories;

        try
        {
            List<biz.turnonline.ecosystem.payment.service.model.Category> dbCategories = service.getCategories();
            categories = mapper.mapAsList( dbCategories, Category.class );
        }
        catch ( Exception e )
        {
            logger.error( "Category list retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "User", authUser.getId() )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return categories;
    }

    @ApiMethod( name = "category.get", path = "categories/{id}", httpMethod = ApiMethod.HttpMethod.GET )
    public Category getCategory( @Named( "id" ) Long id, User authUser )
            throws Exception
    {
        common.authorize( authUser );

        biz.turnonline.ecosystem.payment.service.model.Category dbCategory = getByIdOrThrowNotFoundException( id, authUser );

        return map( dbCategory, authUser );
    }

    @ApiMethod( name = "category.insert", path = "categories", httpMethod = ApiMethod.HttpMethod.POST )
    public Category createCategory( Category category, User authUser )
            throws Exception
    {
        common.authorize( authUser );

        biz.turnonline.ecosystem.payment.service.model.Category dbCategory = mapper.map( category, biz.turnonline.ecosystem.payment.service.model.Category.class );
        service.create( dbCategory );

        return map( dbCategory, authUser );
    }

    @ApiMethod( name = "category.update", path = "categories/{id}", httpMethod = ApiMethod.HttpMethod.PUT )
    public Category updateCategory( Category category, @Named( "id" ) Long id, User authUser )
            throws Exception
    {
        common.authorize( authUser );

        biz.turnonline.ecosystem.payment.service.model.Category dbCategory = getByIdOrThrowNotFoundException( id, authUser );
        mapper.map( category, dbCategory );

        service.update( dbCategory );

        return map( dbCategory, authUser );
    }

    @ApiMethod( name = "category.delete", path = "categories/{id}", httpMethod = ApiMethod.HttpMethod.DELETE )
    public void deleteCategory( @Named( "id" ) Long id, User authUser )
            throws Exception
    {
        common.authorize( authUser );

        biz.turnonline.ecosystem.payment.service.model.Category dbCategory = getByIdOrThrowNotFoundException( id, authUser );

        service.delete( dbCategory );
    }

    @ApiMethod( name = "category.transaction.list", path = "categories/transactions/{transactionId}", httpMethod = ApiMethod.HttpMethod.GET )
    public List<Category> listCategoriesForTransaction( @Named( "transactionId" ) Long transactionId, User authUser )
            throws Exception
    {
        common.authorize( authUser );
        List<Category> categories;

        try
        {
            CommonTransaction transaction = config.getTransaction( transactionId );

            List<biz.turnonline.ecosystem.payment.service.model.TransactionCategory> dbCategories = service.resolveCategories( transaction);
            categories = mapper.mapAsList( dbCategories, Category.class );
        }
        catch ( Exception e )
        {
            logger.error( "Category list retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "User", authUser.getId() )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        return categories;
    }

    // -- private helpers

    private Category map( biz.turnonline.ecosystem.payment.service.model.Category dbCategory, User authUser )
            throws InternalServerErrorException
    {
        try
        {
            return mapper.map( dbCategory, Category.class );
        }
        catch ( Exception e )
        {
            logger.error( "Category mapping has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "User", authUser.getId() )
                    .add( "id", dbCategory.getId() )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }
    }

    private biz.turnonline.ecosystem.payment.service.model.Category getByIdOrThrowNotFoundException( Long id, User authUser )
            throws InternalServerErrorException, NotFoundException
    {
        biz.turnonline.ecosystem.payment.service.model.Category dbCategory;

        try
        {
            dbCategory = service.getById( id );
        }
        catch ( Exception e )
        {
            logger.error( "Category single record retrieval has failed: "
                    + MoreObjects.toStringHelper( "Input" )
                    .add( "User", authUser.getId() )
                    .add( "id", id )
                    .toString(), e );

            throw new InternalServerErrorException( tryAgainLaterMessage() );
        }

        if ( dbCategory == null )
        {
            throw new NotFoundException( categoryNotFoundMessage( id ) );
        }

        return dbCategory;
    }
}
