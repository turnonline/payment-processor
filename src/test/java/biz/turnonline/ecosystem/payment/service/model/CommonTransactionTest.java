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

package biz.turnonline.ecosystem.payment.service.model;

import mockit.Tested;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * {@link CommonTransaction} unit testing.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 */
public class CommonTransactionTest
{
    @Tested
    private CommonTransaction tested;

    @Test
    public void isAmount_AllNull()
    {
        tested.amount( null )
                .currency( null )
                .billAmount( null )
                .billCurrency( null );

        assertWithMessage( "Transaction has amount defined" )
                .that( tested.isAmount() )
                .isFalse();
    }

    @Test
    public void isAmount_AmountAndCurrency()
    {
        tested.amount( 9.0 ).currency( "EUR" );
        assertWithMessage( "Transaction has amount defined" )
                .that( tested.isAmount() )
                .isTrue();
    }

    @Test
    public void isAmount_BillAmountAndBillCurrency()
    {
        tested.billAmount( 10.0 ).billCurrency( "EUR" );
        assertWithMessage( "Transaction has amount defined" )
                .that( tested.isAmount() )
                .isTrue();
    }

    @Test
    public void isAmount_AllDefined()
    {
        tested.amount( 11.5 )
                .currency( "USD" )
                .billAmount( 10.0 )
                .billCurrency( "EUR" );

        assertWithMessage( "Transaction has amount defined" )
                .that( tested.isAmount() )
                .isTrue();
    }

    @Test
    public void propagate_CategoriesAreEmpty()
    {
        assertThat( tested.propagate() ).isTrue();
    }

    @Test
    public void propagate_CategoryPropagate()
    {
        TransactionCategory category = new TransactionCategory();
        category.setPropagate( true );
        tested.getCategories().add( category );

        assertThat( tested.propagate() ).isTrue();
    }

    @Test
    public void propagate_CategoryDoNotPropagate()
    {
        TransactionCategory category = new TransactionCategory();
        category.setPropagate( false );
        tested.getCategories().add( category );

        assertThat( tested.propagate() ).isFalse();
    }

    @Test
    public void propagate_MultipleCategoriesOneNotPropagate()
    {
        TransactionCategory categoryPropagate = new TransactionCategory();
        categoryPropagate.setPropagate( true );
        tested.getCategories().add( categoryPropagate );

        TransactionCategory categoryDoNotPropagate = new TransactionCategory();
        categoryDoNotPropagate.setPropagate( false );
        tested.getCategories().add( categoryDoNotPropagate );

        assertThat( tested.propagate() ).isFalse();
    }

    @Test
    public void propagate_MultipleCategoriesAllNotPropagate()
    {
        TransactionCategory categoryPropagate = new TransactionCategory();
        categoryPropagate.setPropagate( false );
        tested.getCategories().add( categoryPropagate );

        TransactionCategory categoryDoNotPropagate = new TransactionCategory();
        categoryDoNotPropagate.setPropagate( false );
        tested.getCategories().add( categoryDoNotPropagate );

        assertThat( tested.propagate() ).isFalse();
    }

    @Test
    public void propagate_MultipleCategoriesAllPropagate()
    {
        TransactionCategory categoryPropagate = new TransactionCategory();
        categoryPropagate.setPropagate( true );
        tested.getCategories().add( categoryPropagate );

        TransactionCategory categoryDoNotPropagate = new TransactionCategory();
        categoryDoNotPropagate.setPropagate( true );
        tested.getCategories().add( categoryDoNotPropagate );

        assertThat( tested.propagate() ).isTrue();
    }
}