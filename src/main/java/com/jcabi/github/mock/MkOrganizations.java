/**
 * Copyright (c) 2013-2015, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.github.mock;

import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.github.Github;
import com.jcabi.github.Organization;
import com.jcabi.github.Organizations;
import com.jcabi.github.User;
import com.jcabi.xml.XML;
import java.io.IOException;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.xembly.Directives;

/**
 * Github organizations.
 * @author Paul Polishchuk (ppol@ua.fm)
 * @version $Id$
 * @see <a href="http://developer.github.com/v3/orgs/">Organizations API</a>
 * @since 0.7
 * @checkstyle MultipleStringLiteralsCheck (200 lines)
 * @checkstyle ClassDataAbstractionCoupling (200 lines)
 */
@Immutable
@Loggable(Loggable.DEBUG)
@ToString
@EqualsAndHashCode(of = { "storage", "self" })
@SuppressWarnings("PMD.TooManyMethods")
final class MkOrganizations implements Organizations {
    /**
     * Storage.
     */
    private final transient MkStorage storage;

    /**
     * Login of the user logged in.
     */
    private final transient String self;

    /**
     * Public ctor.
     * @param stg Storage
     * @param login User to login
     * @throws IOException If there is any I/O problem
     */
    MkOrganizations(
        @NotNull(message = "stg can't be NULL") final MkStorage stg,
        @NotNull(message = "login can't be NULL") final String login
    )
        throws IOException {
        this.storage = stg;
        this.self = login;
        this.storage.apply(
            new Directives().xpath("/github").addIf("orgs")
        );
    }

    @Override
    @NotNull(message = "Github is never NULL")
    public Github github() {
        return new MkGithub(this.storage, this.self);
    }

    @Override
    @NotNull(message = "User is never NULL")
    public User user() {
        try {
            return new MkUser(this.storage, this.self);
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    @NotNull(message = "org is never NULL")
    public Organization get(
        @NotNull(message = "login is never NULl") final String login
    ) {
        try {
            this.storage.apply(
                new Directives()
                    .xpath(
                        String.format(
                            "/github/orgs[not(org[login='%s'])]",
                            login
                    )
                )
                    .add("org")
                    .add("login").set(login).up()
                    .add("members").up()
            );
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
        return new MkOrganization(this.storage, login);
    }

    @Override
    @NotNull(message = "Iterable of orgs is never NULL")
    public Iterable<Organization> iterate() {
        return new MkIterable<Organization>(
            this.storage,
            String.format("%s/org", this.xpath()),
            new MkIterable.Mapping<Organization>() {
                @Override
                public Organization map(final XML xml) {
                    return MkOrganizations.this.get(
                        xml.xpath("login/text()").get(0)
                    );
                }
            }
        );
    }

    @Override
    @NotNull(message = "Iterable of orgs is never NULL")
    public Iterable<Organization> iterate(
        @NotNull(message = "login is never NULL") final String login
    ) {
        return new MkIterable<Organization>(
            this.storage,
            String.format("%s/org/login", this.xpath()),
            new MkIterable.Mapping<Organization>() {
                @Override
                public Organization map(final XML xml) {
                    return MkOrganizations.this.get(
                        xml.xpath("login/text()").get(0)
                    );
                }
            }
        );
    }

    /**
     * XPath of this element in XML tree.
     * @return XPath
     */
    @NotNull(message = "Xpath is never NULL")
    private String xpath() {
        return "/github/orgs";
    }
}
