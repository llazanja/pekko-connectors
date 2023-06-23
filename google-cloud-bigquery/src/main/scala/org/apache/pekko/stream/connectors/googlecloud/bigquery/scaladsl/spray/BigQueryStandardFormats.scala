/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl.spray

import spray.json.{ AdditionalFormats, StandardFormats }

/**
 * Provides the BigQueryJsonFormats for the non-collection standard types.
 */
trait BigQueryStandardFormats extends StandardFormats { this: AdditionalFormats =>

  implicit def bigQueryOptionFormat[T: BigQueryJsonFormat]: BigQueryJsonFormat[Option[T]] =
    new OptionFormat[T] with BigQueryJsonFormat[Option[T]]

}
