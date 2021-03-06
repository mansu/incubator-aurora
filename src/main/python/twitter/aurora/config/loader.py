import json
import pkgutil
import textwrap

from twitter.aurora.config.schema import base as base_schema

from pystachio.config import Config as PystachioConfig


class AuroraConfigLoader(PystachioConfig):
  SCHEMA_MODULES = []

  @classmethod
  def assembled_schema(cls, schema_modules):
    default_schema = [super(AuroraConfigLoader, cls).DEFAULT_SCHEMA]
    default_schema.extend('from %s import *' % module.__name__ for module in schema_modules)
    return '\n'.join(default_schema)

  @classmethod
  def register_schema(cls, schema_module):
    """Register the schema defined in schema_module, equivalent to doing

         from schema_module.__name__ import *

       before all pystachio configurations are evaluated.
    """
    cls.SCHEMA_MODULES.append(schema_module)
    cls.DEFAULT_SCHEMA = cls.assembled_schema(cls.SCHEMA_MODULES)

  @classmethod
  def register_schemas_from(cls, package):
    """Register schemas from all modules in a particular package."""
    for _, submodule, is_package in pkgutil.iter_modules(package.__path__):
      if is_package:
        continue
      cls.register_schema(
          __import__('%s.%s' % (package.__name__, submodule), fromlist=[package.__name__]))

  @classmethod
  def flush_schemas(cls):
    """Flush all schemas from AuroraConfigLoader.  Intended for test use only."""
    cls.SCHEMA_MODULES = []
    cls.register_schema(base_schema)

  @classmethod
  def load(cls, loadable):
    return cls.load_raw(loadable).environment

  @classmethod
  def load_raw(cls, loadable):
    return cls(loadable)

  @classmethod
  def load_json(cls, filename):
    with open(filename) as fp:
      return base_schema.Job.json_load(fp)

  @classmethod
  def loads_json(cls, string):
    return base_schema.Job(json.loads(string))


AuroraConfigLoader.flush_schemas()
