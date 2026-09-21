package controllers.helpers

import models.PaginationParams
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

object PaginationRedirect {
  
  def redirect(
                pagination : PaginationParams,
                parent : Call,
                page : Int => Call
              ): Option[Result] =
    if (pagination.totalRecords == 0) {
      Some(Redirect(parent))
    } else if (
      pagination.pageNo < 1 ||
        pagination.pageNo > pagination.totalPages
    ) {
      Some(Redirect(page(pagination.totalPages)))
    } else {
      None
    }
    
}
