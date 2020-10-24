package org.apache.maven.plugins.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Adds a single artifact to the project's pom file.
 */
@Mojo( name = "add", requiresProject = false, threadSafe = true )
public class AddMojo
    extends AbstractMojo
{
    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    private MavenSession session;

    private final DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();

    /**
     * The groupId of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "groupId" )
    private String groupId;

    /**
     * The artifactId of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "artifactId" )
    private String artifactId;

    /**
     * The version of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "version" )
    private String version;

    /**
     * The classifier of the artifact to download. Ignored if {@link #artifact} is used.
     *
     * @since 2.3
     */
    @Parameter( property = "classifier" )
    private String classifier;

    /**
     * The packaging of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "packaging", defaultValue = "jar" )
    private String packaging = "jar";

    /**
     * A string of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter( property = "artifact" )
    private String artifact;

    /**
     * Skip plugin execution completely.
     */
    @Parameter( property = "mdep.skip", defaultValue = "false" )
    private boolean skip;

    @Override
    public void execute()
        throws MojoFailureException
    {
        if ( isSkip() )
        {
            getLog().info( "Skipping plugin execution" );
            return;
        }

        if ( coordinate.getArtifactId() == null && artifact == null )
        {
            throw new MojoFailureException( "You must specify an artifact, "
                + "e.g. -Dartifact=org.apache.maven.plugins:maven-downloader-plugin:1.0" );
        }
        if ( artifact != null )
        {
            String[] tokens = StringUtils.split( artifact, ":" );
            if ( tokens.length < 3 || tokens.length > 5 )
            {
                throw new MojoFailureException( "Invalid artifact, you must specify "
                    + "groupId:artifactId:version[:packaging[:classifier]] " + artifact );
            }
            coordinate.setGroupId( tokens[0] );
            coordinate.setArtifactId( tokens[1] );
            coordinate.setVersion( tokens[2] );
            if ( tokens.length >= 4 )
            {
                coordinate.setType( tokens[3] );
            }
            if ( tokens.length == 5 )
            {
                coordinate.setClassifier( tokens[4] );
            }
        }

        MavenProject currentProject = session.getCurrentProject();
        if ( currentProject == null )
        {
            throw new MojoFailureException( "You must execute this goal in a project" );
        }

        File moduleProjectFile = currentProject.getFile();
        if ( moduleProjectFile == null )
        {
            throw new MojoFailureException( "No pom file for this project found" );
        }

        Dependency dependency = mapDependencyFromCoordinate( coordinate );
        currentProject.getOriginalModel().addDependency( dependency );
        try
        {
            // TODO: Using this writer only works for pom.xml files.
            //   We should rather use org.apache.maven.model.io.ModelWriter instead (but is not injectable atm?).
            MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
            // TODO: The pom file will be completely shuffled around, which is not very user-friendly. Can we fix that?
            mavenXpp3Writer.write( new FileWriter( moduleProjectFile ), currentProject.getOriginalModel() );

            String dependencyKey = dependency.getManagementKey();
            getLog().info( "Successfully added " + dependencyKey + " to " + moduleProjectFile.getName() );


        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Writing to file failed: " + moduleProjectFile.getName(), e );
        }
    }

    private static Dependency mapDependencyFromCoordinate( DependableCoordinate coordinate )
    {
        Dependency dependency = new Dependency();
        dependency.setGroupId( coordinate.getGroupId() );
        dependency.setArtifactId( coordinate.getArtifactId() );
        dependency.setVersion( coordinate.getVersion() );
        dependency.setType( coordinate.getType() );
        if ( coordinate.getClassifier() != null )
        {
            dependency.setClassifier( dependency.getClassifier() );
        }
        return dependency;
    }

    /**
     * @return {@link #skip}
     */
    protected boolean isSkip()
    {
        return skip;
    }

    /**
     * @param groupId The groupId.
     */
    public void setGroupId( String groupId )
    {
        this.coordinate.setGroupId( groupId );
    }

    /**
     * @param artifactId The artifactId.
     */
    public void setArtifactId( String artifactId )
    {
        this.coordinate.setArtifactId( artifactId );
    }

    /**
     * @param version The version.
     */
    public void setVersion( String version )
    {
        this.coordinate.setVersion( version );
    }

    /**
     * @param classifier The classifier to be used.
     */
    public void setClassifier( String classifier )
    {
        this.coordinate.setClassifier( classifier );
    }

    /**
     * @param type packaging.
     */
    public void setPackaging( String type )
    {
        this.coordinate.setType( type );
    }

}
