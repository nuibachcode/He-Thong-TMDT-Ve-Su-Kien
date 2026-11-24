package vn.edu.tmdt.catalog.service;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import vn.edu.tmdt.catalog.domain.Counter;

@Service
public class SequenceService {

    private final MongoTemplate mongo;

    public SequenceService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public long next(String name) {
        Counter counter = mongo.findAndModify(
                Query.query(Criteria.where("_id").is(name)),
                new Update().inc("seq", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Counter.class);
        return counter == null ? 1 : counter.seq;
    }
}
