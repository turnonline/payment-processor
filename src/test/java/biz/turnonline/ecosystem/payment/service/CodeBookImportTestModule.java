package biz.turnonline.ecosystem.payment.service;

import com.google.inject.AbstractModule;
import org.ctoolkit.agent.config.LocalAgentModule;
import org.ctoolkit.agent.service.ChangeSetService;
import org.ctoolkit.agent.service.DataAccess;
import org.ctoolkit.agent.service.EntityPool;
import org.ctoolkit.agent.service.impl.ChangeSetEntityToEntityMapper;
import org.ctoolkit.agent.service.impl.ChangeSetServiceBean;
import org.ctoolkit.agent.service.impl.DataAccessBean;
import org.ctoolkit.agent.service.impl.EntityEncoder;
import org.ctoolkit.agent.service.impl.EntityPoolThreadLocal;
import org.ctoolkit.agent.service.impl.ImportTask;

import javax.inject.Singleton;

/**
 * The guice module configuration to let import dataset by 'ctoolkit-agent-import'.
 *
 * @author <a href="mailto:medvegy@turnonline.biz">Aurel Medvegy</a>
 * @see LocalAgentModule
 */
public class CodeBookImportTestModule
        extends AbstractModule
{
    @Override
    protected void configure()
    {
        // taken from LocalAgentModule and EntityPool scope has changed to Singleton
        bind( ChangeSetEntityToEntityMapper.class ).in( Singleton.class );
        bind( EntityEncoder.class ).in( Singleton.class );
        bind( EntityPool.class ).to( EntityPoolThreadLocal.class ).in( Singleton.class );
        bind( DataAccess.class ).to( DataAccessBean.class ).in( Singleton.class );
        bind( ChangeSetService.class ).to( ChangeSetServiceBean.class ).in( Singleton.class );
        requestStaticInjection( ImportTask.class );
    }
}
