
package org.jdw.blog.receiver;

import org.jdw.blog.common.domain.PayloadType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiveCountRepository extends JpaRepository<ReceiveCount, PayloadType> {

}
