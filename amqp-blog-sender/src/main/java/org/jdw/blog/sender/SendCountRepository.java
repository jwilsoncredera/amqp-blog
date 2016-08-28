
package org.jdw.blog.sender;

import org.jdw.blog.common.domain.PayloadType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SendCountRepository extends JpaRepository<SendCount, PayloadType> {

}
